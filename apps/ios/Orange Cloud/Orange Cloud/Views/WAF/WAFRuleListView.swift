//
//  WAFRuleListView.swift
//  Orange Cloud
//
//  WAF 自定义规则：查看 / 新建 / 删除 / 启停，写操作按 zone-waf.write 门控。
//

import SwiftUI

struct WAFRuleListView: View {

    let zoneName: String

    @Environment(AuthManager.self) private var auth
    @State private var viewModel: WAFRulesViewModel
    @State private var showDenied = false
    @State private var showForm = false
    @State private var ruleToDelete: WAFRule?

    init(zoneId: String, zoneName: String, session: SessionStore) {
        self.zoneName = zoneName
        _viewModel = State(initialValue: WAFRulesViewModel(service: session.wafService, zoneId: zoneId))
    }

    private var canWrite: Bool { auth.hasScope("zone-waf.write") }

    var body: some View {
        Group {
            if viewModel.isLoading && !viewModel.loaded {
                SkeletonList(rows: 6, icon: .none, trailing: true)
            } else if viewModel.rules.isEmpty {
                ContentUnavailableView {
                    Label("没有自定义规则", systemImage: "shield")
                } description: {
                    Text(canWrite
                         ? String(localized: "点击右上角 + 创建第一条防火墙规则")
                         : String(localized: "在 Cloudflare Dashboard → 安全性 → WAF 中创建自定义规则"))
                } actions: {
                    if canWrite {
                        Button("添加规则") { showForm = true }
                            .buttonStyle(.borderedProminent)
                            .tint(Color.ocOrangePressed)
                            .fontWeight(.bold)
                    }
                }
            } else {
                List {
                    Section {
                        ForEach(viewModel.rules) { rule in
                            WAFRuleRow(
                                rule: rule,
                                canWrite: canWrite,
                                isToggling: viewModel.togglingRuleId == rule.id,
                                onToggle: { enabled in
                                    Task { await viewModel.toggle(rule: rule, enabled: enabled) }
                                },
                                onDenied: { showDenied = true }
                            )
                            .swipeActions(edge: .trailing) {
                                Button(role: .destructive) {
                                    if canWrite {
                                        ruleToDelete = rule
                                    } else {
                                        showDenied = true
                                    }
                                } label: {
                                    Label("删除", systemImage: "trash")
                                }
                            }
                        }
                    } footer: {
                        Text(canWrite
                             ? String(localized: "规则按从上到下的顺序执行，左滑可删除。")
                             : String(localized: "当前授权仅限读取（zone-waf.read），无法修改规则。"))
                    }
                    .glassRow()
                }
                .scrollContentBackground(.hidden)
                .refreshable { await viewModel.load() }
            }
        }
        .background { SkyBackground() }
        .navigationTitle("WAF 防火墙")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button("添加", systemImage: "plus") {
                    if canWrite {
                        showForm = true
                    } else {
                        showDenied = true
                    }
                }
            }
        }
        .sheet(isPresented: $showForm) {
            WAFRuleFormView(viewModel: viewModel)
        }
        .task { await viewModel.load() }
        .confirmationDialog(
            "删除规则",
            isPresented: .init(
                get: { ruleToDelete != nil },
                set: { if !$0 { ruleToDelete = nil } }
            ),
            titleVisibility: .visible
        ) {
            if let rule = ruleToDelete {
                Button("删除「\(rule.description ?? String(localized: "未命名规则"))」", role: .destructive) {
                    Task { await viewModel.delete(rule: rule) }
                }
            }
        } message: {
            Text("此操作不可撤销，规则将立即停止生效。")
        }
        .alert("权限不足", isPresented: $showDenied) {
            Button("好", role: .cancel) {}
        } message: {
            Text("当前授权未包含 WAF 编辑权限（zone-waf.write）。\n请在设置中退出登录后重新授权以启用此功能。")
        }
        .alert("出错了", isPresented: .init(
            get: { viewModel.error != nil && !showForm },
            set: { if !$0 { viewModel.error = nil } }
        )) {
            Button("好", role: .cancel) {}
        } message: {
            Text(viewModel.error ?? "")
        }
    }
}

// MARK: - 新建规则表单

private struct WAFRuleFormView: View {

    let viewModel: WAFRulesViewModel

    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var action: WAFRuleAction = .block
    @State private var expression = ""
    @State private var enabled = true

    // 设备端 AI 生成
    @State private var nlPrompt = ""
    @State private var readback: String?

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty
            && !expression.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                if WAFAssistant.isReady {
                    aiSection
                }

                Section("规则") {
                    TextField("规则名称", text: $name)
                    Picker("动作", selection: $action) {
                        ForEach(WAFRuleAction.allCases) { action in
                            Text(action.label).tag(action)
                        }
                    }
                    Toggle("启用", isOn: $enabled)
                }

                Section {
                    TextEditor(text: $expression)
                        .font(.callout.monospaced())
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .frame(minHeight: 100)
                } header: {
                    Text("表达式")
                } footer: {
                    Text("Cloudflare Rules 语言，例如：\n(http.request.uri.path contains \"/admin\") or (ip.src eq 198.51.100.4)")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("新建规则")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("保存").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
            .onDisappear {
                viewModel.error = nil
                viewModel.generationError = nil
            }
        }
    }

    // MARK: - 设备端 AI 生成

    @ViewBuilder
    private var aiSection: some View {
        Section {
            TextField(
                String(localized: "例如：拦截来自中国大陆、访问 /admin 的请求"),
                text: $nlPrompt,
                axis: .vertical
            )
            .lineLimit(2...5)

            Button {
                Task { await generate() }
            } label: {
                HStack(spacing: 8) {
                    if viewModel.isGenerating {
                        ProgressView().controlSize(.small)
                        Text("生成中…")
                    } else {
                        Image(systemName: "sparkles")
                        Text("生成表达式")
                    }
                }
                .frame(maxWidth: .infinity)
            }
            .disabled(nlPrompt.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || viewModel.isGenerating)

            if let readback {
                Label {
                    Text(readback)
                        .font(.footnote)
                        .foregroundStyle(.primary)
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "checkmark.seal.fill")
                        .foregroundStyle(Color.ocOrangeText)
                }
            }

            if let genError = viewModel.generationError {
                Text(genError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        } header: {
            Label("用自然语言描述", systemImage: "sparkles")
        } footer: {
            Text(readback == nil
                 ? String(localized: "在设备上离线生成，描述会填入下方表达式，提交前请核对。")
                 : String(localized: "已填入下方表达式，并暂时关闭了「启用」，确认无误后再开启保存。"))
        }
    }

    private func generate() async {
        guard let result = await viewModel.generate(from: nlPrompt) else {
            readback = nil
            return
        }
        withAnimation(.smooth) {
            expression = result.expression
            action = result.action
            enabled = false        // AI 生成默认不启用，人在回路确认后再开
            readback = result.summary
        }
    }

    private func save() async {
        viewModel.error = nil
        let trimmedExpression = expression.trimmingCharacters(in: .whitespacesAndNewlines)
        if let problem = WAFExpressionLint.problem(in: trimmedExpression) {
            viewModel.error = problem
            return
        }
        let draft = WAFRuleCreate(
            action: action.rawValue,
            expression: trimmedExpression,
            description: name.trimmingCharacters(in: .whitespaces),
            enabled: enabled
        )
        if await viewModel.addRule(draft) {
            dismiss()
        }
    }
}

// MARK: - 规则行

private struct WAFRuleRow: View {

    let rule: WAFRule
    let canWrite: Bool
    let isToggling: Bool
    let onToggle: (Bool) -> Void
    let onDenied: () -> Void

    @State private var explanation: String?
    @State private var isExplaining = false
    @State private var explainError: String?

    private var actionColor: Color {
        switch rule.action {
        case "block":                                    .red
        case "challenge", "managed_challenge", "js_challenge": .orange
        case "log":                                      .blue
        case "allow", "skip":                            .green
        default:                                         .gray
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Text(rule.description ?? String(localized: "未命名规则"))
                    .font(.callout.weight(.semibold))
                    .lineLimit(1)
                Spacer()
                Text(rule.actionText)
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(actionColor)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 3)
                    .background(actionColor.opacity(0.14), in: Capsule())
                if isToggling {
                    ProgressView()
                        .controlSize(.small)
                } else if canWrite {
                    Toggle("", isOn: Binding(
                        get: { rule.enabled ?? true },
                        set: { onToggle($0) }
                    ))
                    .labelsHidden()
                    .accessibilityLabel("启用规则")
                } else {
                    Button {
                        onDenied()
                    } label: {
                        Image(systemName: (rule.enabled ?? true) ? "checkmark.circle.fill" : "circle")
                            .foregroundStyle((rule.enabled ?? true) ? .green : .secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel((rule.enabled ?? true) ? "已启用" : "已停用")
                    .accessibilityHint("需要写入权限才能修改")
                }
            }
            if let expression = rule.expression {
                Text(expression)
                    .font(.caption.monospaced())
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                    .truncationMode(.tail)

                if WAFAssistant.isReady, !expression.isEmpty {
                    explanationView(for: expression)
                }
            }
        }
        .padding(.vertical, 4)
        .opacity((rule.enabled ?? true) ? 1 : 0.5)
    }

    /// 反向能力：按需把表达式翻译成大白话（设备端、只读）。
    @ViewBuilder
    private func explanationView(for expression: String) -> some View {
        if let explanation {
            Label {
                Text(explanation)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "sparkles")
                    .font(.caption2)
                    .foregroundStyle(Color.ocOrangeText)
            }
            .padding(.top, 2)
            .transition(.opacity)
        } else {
            Button {
                Task { await explain(expression) }
            } label: {
                HStack(spacing: 4) {
                    if isExplaining {
                        ProgressView().controlSize(.mini)
                    } else {
                        Image(systemName: "sparkles")
                    }
                    Text(isExplaining ? String(localized: "解释中…") : String(localized: "用大白话解释"))
                }
                .font(.caption2.weight(.medium))
                .foregroundStyle(Color.ocOrangeText)
            }
            .buttonStyle(.borderless)
            .disabled(isExplaining)
            .padding(.top, 1)

            if let explainError {
                Text(explainError)
                    .font(.caption2)
                    .foregroundStyle(.red)
            }
        }
    }

    private func explain(_ expression: String) async {
        guard !isExplaining else { return }
        isExplaining = true
        explainError = nil
        defer { isExplaining = false }
        do {
            let result = try await WAFAssistant.explainRule(expression: expression, action: rule.action)
            withAnimation(.smooth) { explanation = result }
        } catch {
            explainError = error.localizedDescription
        }
    }
}
