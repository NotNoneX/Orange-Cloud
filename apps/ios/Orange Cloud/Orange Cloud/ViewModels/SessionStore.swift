//
//  SessionStore.swift
//  Orange Cloud
//
//  登录后的会话容器：持有 CFAPIClient 与各 Service，管理账号选择。
//  P0 单账号场景默认选中第一个账号。
//

import Foundation
import Observation

@Observable
@MainActor
final class SessionStore {

    let accountService:    AccountService
    let zoneService:       ZoneService
    let dnsService:        DNSService
    let workerService:     WorkerService
    let workerTailService: WorkerTailService
    let analyticsService:  AnalyticsService
    let r2Service:         R2Service
    let d1Service:         D1Service
    let kvService:         KVService
    let tunnelService:     TunnelService
    let wafService:        WAFService
    let snippetService:    SnippetService
    let zoneSettingsService: ZoneSettingsService
    let sslCertificateService:     SSLCertificateService
    let transformRuleService:      TransformRuleService
    let firewallAccessRuleService: FirewallAccessRuleService
    let cacheRuleService:          CacheRuleService
    let pagesService:              PagesService
    let loadBalancerService:       LoadBalancerService
    let bulkRedirectService:       BulkRedirectService
    let auditLogService:           AuditLogService
    let emailRoutingService:       EmailRoutingService
    let rateLimitService:          RateLimitService
    let zeroTrustService:          ZeroTrustService

    var accounts: [Account] = []
    var selectedAccount: Account? {
        didSet {
            // Widget 自取用量数据需要知道当前账户
            UserDefaults(suiteName: WidgetSnapshot.appGroupID)?
                .set(selectedAccount?.id, forKey: "currentAccountId")
        }
    }
    var isLoadingAccounts = false
    var error: String?

    private let authManager: AuthManager
    /// 本会话对应的登录身份（SessionStore 按身份重建，加载完成回填账号名时
    /// 不能临时取 currentSessionId——异步期间用户可能已切到别的身份）
    private let sessionId: UUID?

    init(authManager: AuthManager) {
        self.authManager = authManager
        self.sessionId = authManager.currentSessionId
        let client = CFAPIClient(authManager: authManager)
        self.accountService    = AccountService(client: client)
        self.zoneService       = ZoneService(client: client)
        self.dnsService        = DNSService(client: client)
        self.workerService     = WorkerService(client: client)
        self.workerTailService = WorkerTailService(client: client)
        self.analyticsService  = AnalyticsService(client: client)
        self.r2Service         = R2Service(client: client)
        self.d1Service         = D1Service(client: client)
        self.kvService         = KVService(client: client)
        self.tunnelService     = TunnelService(client: client)
        self.wafService        = WAFService(client: client)
        self.snippetService    = SnippetService(client: client)
        self.zoneSettingsService = ZoneSettingsService(client: client)
        self.sslCertificateService     = SSLCertificateService(client: client)
        self.transformRuleService      = TransformRuleService(client: client)
        self.firewallAccessRuleService = FirewallAccessRuleService(client: client)
        self.cacheRuleService          = CacheRuleService(client: client)
        self.pagesService              = PagesService(client: client)
        self.loadBalancerService       = LoadBalancerService(client: client)
        self.bulkRedirectService       = BulkRedirectService(client: client)
        self.auditLogService           = AuditLogService(client: client)
        self.emailRoutingService       = EmailRoutingService(client: client)
        self.rateLimitService          = RateLimitService(client: client)
        self.zeroTrustService          = ZeroTrustService(client: client)
    }

    /// 幂等加载账号列表，首个账号设为当前账号
    func ensureAccounts() async {
        guard accounts.isEmpty, !isLoadingAccounts else { return }
        isLoadingAccounts = true
        error = nil
        do {
            accounts = try await accountService.listAccounts()
            if selectedAccount == nil {
                selectedAccount = accounts.first
            }
            // 登录身份的展示名同步为真实账号名（设置页与 Dashboard 一致）
            if let name = accounts.first?.name, let sessionId {
                authManager.updateSessionLabel(name, for: sessionId)
            }
            // 把本身份的账号并入 Widget 账号目录（小组件「选择账号」picker 数据源）
            if let sessionId {
                WidgetDataStore.mergeAccounts(
                    accounts.map { WidgetAccount(id: $0.id, name: $0.name, sessionId: sessionId.uuidString) },
                    sessionId: sessionId.uuidString
                )
            }
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingAccounts = false
    }
}
