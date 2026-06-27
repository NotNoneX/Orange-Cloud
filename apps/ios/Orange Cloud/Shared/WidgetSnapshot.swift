//
//  WidgetSnapshot.swift
//  Orange Cloud（主 App 与 Widget Extension 共享）
//
//  App 刷新 Zone 后写入 App Group，Widget 时间线读取展示。
//  数据按 Cloudflare 账号（accountId）分桶存储：Widget 可在配置里固定某个账号，
//  未配置时回退「当前账号」指针（currentAccountId，随 App 内切账号更新）。
//

import Foundation

nonisolated struct WidgetSnapshot: Codable, Sendable {

    var accountId:   String?     // 所属账号；旧快照可能为 nil（按当前账号回退）
    var accountName: String
    var totalZones:  Int
    var activeZones: Int
    var updatedAt:   Date

    static let appGroupID = "group.jiamin.chen.Orange-Cloud"
    private static let legacyKey  = "widgetSnapshot"
    private static let byAccountKey = "widgetOverviewByAccount"
    static let currentAccountKey = "currentAccountId"

    private static func defaults() -> UserDefaults? { UserDefaults(suiteName: appGroupID) }

    /// App Group 里记录的「当前账号」accountId（随 App 内切账号更新）
    static func currentAccountId() -> String? {
        defaults()?.string(forKey: currentAccountKey)
    }

    private static func loadMap() -> [String: WidgetSnapshot] {
        guard let d = defaults(), let data = d.data(forKey: byAccountKey),
              let map = try? JSONDecoder().decode([String: WidgetSnapshot].self, from: data) else { return [:] }
        return map
    }

    /// 按账号存一份（同时写旧单键兜底老读取路径）
    func save() {
        guard let d = Self.defaults() else { return }
        if let id = accountId, !id.isEmpty {
            var map = Self.loadMap()
            map[id] = self
            if let data = try? JSONEncoder().encode(map) { d.set(data, forKey: Self.byAccountKey) }
        }
        if let data = try? JSONEncoder().encode(self) { d.set(data, forKey: Self.legacyKey) }
    }

    /// 指定账号的总览；无则回退当前账号 / 旧单键
    static func load(accountId: String?) -> WidgetSnapshot? {
        let map = loadMap()
        if let id = accountId, let snap = map[id] { return snap }
        if let id = currentAccountId(), let snap = map[id] { return snap }
        guard let d = defaults(), let data = d.data(forKey: legacyKey) else { return map.values.first }
        return try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
    }

    /// 当前账号的总览
    static func load() -> WidgetSnapshot? { load(accountId: currentAccountId()) }
}

// MARK: - 可供 Widget 选择的账号目录（picker 数据源，App 加载账号时写入）

nonisolated struct WidgetAccount: Codable, Sendable, Identifiable {
    var id:        String     // Cloudflare accountId（全局唯一）
    var name:      String
    var sessionId: String     // 所属登录身份（自取数时按它取 token）
}

// MARK: - 单 Zone 指标快照（Zone 类 Widget 的数据源，App 刷新时写入）

nonisolated struct WidgetZoneMetrics: Codable, Sendable, Identifiable {
    var id:             String
    var name:           String
    var requests:       Int
    var bytes:          Int
    var threats:        Int
    var uniques:        Int
    var cacheHitRate:   Double?     // 0–100
    var requestsTrend:  Double?     // 与前一个 24h 的百分比变化
    var requestsSeries: [Int]       // 24h 逐小时
    var bytesSeries:    [Int]
    var updatedAt:      Date
    var accountId:      String?     // 所属账号（账号总览按此过滤；旧快照可能为 nil）
}

// MARK: - 用量快照（用量 Widget 的数据源，按服务分组、行级状态条）

nonisolated struct WidgetUsageRow: Codable, Sendable {
    var title:     String     // 如 "请求 · 今日"
    var used:      Int
    var quota:     Int?       // nil = 无参考额度（不画条）
    var valueText: String     // 预格式化的展示值
}

nonisolated struct WidgetUsageService: Codable, Sendable, Identifiable {
    var id:   String          // "workers" | "r2" | "d1" | "kv"
    var name: String
    var rows: [WidgetUsageRow]
}

nonisolated struct WidgetUsageData: Codable, Sendable {
    var services:  [WidgetUsageService]
    var updatedAt: Date
}

// MARK: - App Group 读写

nonisolated enum WidgetDataStore {

    private static let zonesKey = "widgetZoneMetrics"
    private static let usageByAccountKey = "widgetUsageByAccount"
    private static let usageLegacyKey = "widgetUsageData"
    private static let accountsKey = "widgetAccounts"
    private static let analyticsAvailableKey = "accountAnalyticsAvailable"

    private static func defaults() -> UserDefaults? { UserDefaults(suiteName: WidgetSnapshot.appGroupID) }

    // ── 账号目录 ──

    /// 把某身份的账号并入目录（按账号 upsert，先移除该身份的旧条目，保留其它身份）。
    static func mergeAccounts(_ accounts: [WidgetAccount], sessionId: String) {
        guard let d = defaults() else { return }
        var existing = loadAccounts().filter { $0.sessionId != sessionId }
        existing.append(contentsOf: accounts)
        if let data = try? JSONEncoder().encode(existing) { d.set(data, forKey: accountsKey) }
    }

    static func loadAccounts() -> [WidgetAccount] {
        guard let d = defaults(), let data = d.data(forKey: accountsKey),
              let accts = try? JSONDecoder().decode([WidgetAccount].self, from: data) else { return [] }
        return accts
    }

    // ── Zone 指标（按账号 upsert，跨账号并存；zone id 全局唯一）──

    static func saveZones(_ zones: [WidgetZoneMetrics], accountId: String) {
        guard let d = defaults() else { return }
        var merged = loadZones().filter { $0.accountId != accountId }
        merged.append(contentsOf: zones)
        // 同 id 去重（保留后写入的）
        var seen = Set<String>()
        var deduped: [WidgetZoneMetrics] = []
        for zone in merged.reversed() where seen.insert(zone.id).inserted { deduped.append(zone) }
        if let data = try? JSONEncoder().encode(Array(deduped.reversed())) { d.set(data, forKey: zonesKey) }
    }

    /// 整组覆盖写入（Watch 镜像 iPhone 推来的整份快照时用，不做按账号 upsert）
    static func saveAllZones(_ zones: [WidgetZoneMetrics]) {
        guard let d = defaults(), let data = try? JSONEncoder().encode(zones) else { return }
        d.set(data, forKey: zonesKey)
    }

    /// 全部账号的 Zone（Zone 类 Widget 按选中的 zone id 取，跨账号都在）
    static func loadZones() -> [WidgetZoneMetrics] {
        guard let d = defaults(), let data = d.data(forKey: zonesKey),
              let zones = try? JSONDecoder().decode([WidgetZoneMetrics].self, from: data) else { return [] }
        return zones
    }

    /// 指定账号的 Zone（账号总览聚合用）；旧快照无 accountId 时回退全部
    static func loadZones(accountId: String?) -> [WidgetZoneMetrics] {
        guard let id = accountId else { return loadZones() }
        let scoped = loadZones().filter { $0.accountId == id }
        return scoped.isEmpty ? loadZones().filter { $0.accountId == nil } : scoped
    }

    // ── 用量（按账号分桶）──

    private static func loadUsageMap() -> [String: WidgetUsageData] {
        guard let d = defaults(), let data = d.data(forKey: usageByAccountKey),
              let map = try? JSONDecoder().decode([String: WidgetUsageData].self, from: data) else { return [:] }
        return map
    }

    static func saveUsage(_ usage: WidgetUsageData, accountId: String) {
        guard let d = defaults() else { return }
        if !accountId.isEmpty {
            var map = loadUsageMap()
            map[accountId] = usage
            if let data = try? JSONEncoder().encode(map) { d.set(data, forKey: usageByAccountKey) }
        }
        if let data = try? JSONEncoder().encode(usage) { d.set(data, forKey: usageLegacyKey) }
    }

    static func loadUsage(accountId: String?) -> WidgetUsageData? {
        let map = loadUsageMap()
        if let id = accountId, let usage = map[id] { return usage }
        if let id = WidgetSnapshot.currentAccountId(), let usage = map[id] { return usage }
        guard let d = defaults(), let data = d.data(forKey: usageLegacyKey) else { return map.values.first }
        return try? JSONDecoder().decode(WidgetUsageData.self, from: data)
    }

    /// 当前账号的用量
    static func loadUsage() -> WidgetUsageData? { loadUsage(accountId: WidgetSnapshot.currentAccountId()) }

    /// 当前账号是否拥有账户级数据（analytics）查询权限。未知时默认 true，避免首次误报不可用。
    static func saveAccountAnalyticsAvailable(_ available: Bool) {
        defaults()?.set(available, forKey: analyticsAvailableKey)
    }

    static func loadAccountAnalyticsAvailable() -> Bool {
        guard let d = defaults(), d.object(forKey: analyticsAvailableKey) != nil else { return true }
        return d.bool(forKey: analyticsAvailableKey)
    }
}
