package jiamin.chen.orangecloud.ui.root

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.ExperimentalMaterial3AdaptiveNavigationSuiteApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jiamin.chen.orangecloud.core.util.launchCustomTab
import jiamin.chen.orangecloud.ui.login.LoginViewModel
import jiamin.chen.orangecloud.ui.paywall.PaywallScreen
import jiamin.chen.orangecloud.ui.paywall.ProGateViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import jiamin.chen.orangecloud.R
import jiamin.chen.orangecloud.core.design.SkyBackground
import jiamin.chen.orangecloud.core.design.SkyPhase
import jiamin.chen.orangecloud.ui.analytics.ZoneAnalyticsScreen
import jiamin.chen.orangecloud.ui.dashboard.DashboardScreen
import jiamin.chen.orangecloud.ui.dns.DnsListScreen
import jiamin.chen.orangecloud.ui.network.TunnelDetailScreen
import jiamin.chen.orangecloud.ui.network.TunnelListScreen
import jiamin.chen.orangecloud.ui.paywall.ProGate
import jiamin.chen.orangecloud.ui.waf.WafRulesScreen
import jiamin.chen.orangecloud.ui.update.UpdateDialog
import jiamin.chen.orangecloud.ui.update.UpdateViewModel
import jiamin.chen.orangecloud.ui.whatsnew.WhatsNewDialog
import jiamin.chen.orangecloud.ui.whatsnew.WhatsNewViewModel
import jiamin.chen.orangecloud.ui.login.LoginScreen
import jiamin.chen.orangecloud.ui.settings.IdentityDetailScreen
import jiamin.chen.orangecloud.ui.settings.SettingsScreen
import jiamin.chen.orangecloud.ui.status.StatusScreen
import jiamin.chen.orangecloud.ui.firewall.ZoneAccessRulesScreen
import jiamin.chen.orangecloud.ui.transform.ZoneTransformScreen
import jiamin.chen.orangecloud.ui.zonesettings.ZonePerformanceScreen
import jiamin.chen.orangecloud.ui.zonesettings.ZoneSettingsScreen
import jiamin.chen.orangecloud.ui.zonesettings.ZoneSslCertsScreen
import jiamin.chen.orangecloud.ui.zonesettings.ZoneSslSettingsScreen
import jiamin.chen.orangecloud.ui.snippets.SnippetEditorScreen
import jiamin.chen.orangecloud.ui.snippets.SnippetsListScreen
import jiamin.chen.orangecloud.ui.storage.D1DatabaseListScreen
import jiamin.chen.orangecloud.ui.storage.D1QueryScreen
import jiamin.chen.orangecloud.ui.storage.D1TableScreen
import jiamin.chen.orangecloud.ui.storage.KVKeyListScreen
import jiamin.chen.orangecloud.ui.storage.KVNamespaceListScreen
import jiamin.chen.orangecloud.ui.storage.R2BucketListScreen
import jiamin.chen.orangecloud.ui.storage.R2BucketSettingsScreen
import jiamin.chen.orangecloud.ui.storage.R2ObjectListScreen
import jiamin.chen.orangecloud.ui.storage.StorageHubScreen
import jiamin.chen.orangecloud.ui.workers.WorkerDetailScreen
import jiamin.chen.orangecloud.ui.workers.WorkerListScreen
import jiamin.chen.orangecloud.ui.workers.WorkerRoutesScreen
import jiamin.chen.orangecloud.ui.workers.WorkerSecretsScreen
import jiamin.chen.orangecloud.ui.workers.WorkerTailScreen
import jiamin.chen.orangecloud.ui.workers.WorkerTriggersScreen
import jiamin.chen.orangecloud.ui.zones.ZoneDetailScreen
import jiamin.chen.orangecloud.ui.zones.ZonesPaneScreen
import java.time.LocalTime

@Composable
fun OrangeCloudRoot(viewModel: RootViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    when {
        !authState.isReady -> SplashScreen()
        authState.isLoggedIn -> MainScaffold()
        else -> LoginScreen()
    }

    // 自助更新提示：仅 sideload 的 direct 包实际触发，覆盖于任意鉴权态之上。
    val updateViewModel: UpdateViewModel = hiltViewModel()
    val update by updateViewModel.available.collectAsStateWithLifecycle()
    val installing by updateViewModel.installing.collectAsStateWithLifecycle()
    val updateContext = LocalContext.current
    update?.let { info ->
        UpdateDialog(
            info = info,
            installing = installing,
            onUpdate = {
                // 应用内安装失败（无权限 / 签名不符 / 下载失败）回退浏览器下载
                updateViewModel.install { url ->
                    runCatching { updateContext.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                }
            },
            onSkip = updateViewModel::skip,
            onDismiss = updateViewModel::dismiss,
        )
    }
}

private enum class TopDestination(val labelRes: Int, val icon: ImageVector) {
    Dashboard(R.string.nav_dashboard, Icons.Outlined.GridView),
    Zones(R.string.nav_zones, Icons.Outlined.Language),
    Workers(R.string.nav_workers, Icons.Outlined.Bolt),
    Storage(R.string.nav_storage, Icons.Outlined.Storage),
    Settings(R.string.nav_settings, Icons.Outlined.Settings),
}

/** 路由表。下钻页（DNS / Worker 详情）也预留 App Shortcuts 深链（Phase E 收尾挂 manifest）。 */
private object Dest {
    const val DASHBOARD = "dashboard"
    const val ZONES = "zones"
    const val WORKERS = "workers"
    const val STORAGE = "storage"
    const val SETTINGS = "settings"
    const val IDENTITY_ROUTE = "identity/{sessionId}"
    const val TUNNELS = "tunnels"
    const val TUNNEL_DETAIL_ROUTE = "tunnel/{tunnelId}?tunnelName={tunnelName}"
    const val STATUS = "status"
    const val PAYWALL = "paywall"
    const val WAF_ROUTE = "waf/{zoneId}?zoneName={zoneName}"
    // 存储下钻
    const val R2_BUCKETS = "r2/buckets"
    const val R2_OBJECTS_ROUTE = "r2/objects/{bucket}"
    const val R2_BUCKET_SETTINGS_ROUTE = "r2/settings/{bucket}"
    const val D1_DATABASES = "d1/databases"
    const val D1_QUERY_ROUTE = "d1/query/{dbId}?dbName={dbName}"
    const val D1_TABLE_ROUTE = "d1/table/{dbId}?table={table}"
    const val KV_NAMESPACES = "kv/namespaces"
    const val KV_KEYS_ROUTE = "kv/keys/{nsId}?nsTitle={nsTitle}"
    const val ZONE_DETAIL_ROUTE = "zone/{zoneId}?zoneName={zoneName}"
    const val DNS_ROUTE = "dns/{zoneId}?zoneName={zoneName}"
    const val ANALYTICS_ROUTE = "analytics/{zoneId}?zoneName={zoneName}"
    const val SNIPPETS_ROUTE = "snippets/{zoneId}?zoneName={zoneName}"
    const val SNIPPET_EDIT_ROUTE = "snippetEdit/{zoneId}?zoneName={zoneName}&name={name}"
    const val ZONE_SETTINGS_ROUTE = "zonesettings/{zoneId}?zoneName={zoneName}"
    const val SSL_ROUTE = "ssl/{zoneId}?zoneName={zoneName}"
    const val SSL_CERTS_ROUTE = "sslcerts/{zoneId}?zoneName={zoneName}"
    const val PERFORMANCE_ROUTE = "performance/{zoneId}?zoneName={zoneName}"
    const val TRANSFORM_ROUTE = "transform/{zoneId}?zoneName={zoneName}"
    const val ACCESS_RULES_ROUTE = "accessrules/{zoneId}?zoneName={zoneName}"
    const val WORKER_ROUTE = "worker/{scriptName}"
    const val WORKER_SECRETS_ROUTE = "worker/{scriptName}/secrets"
    const val WORKER_TRIGGERS_ROUTE = "worker/{scriptName}/triggers"
    const val WORKER_DOMAINS_ROUTE = "worker/{scriptName}/domains"
    const val TAIL_ROUTE = "tail/{scriptName}"
    private fun zoneScoped(prefix: String, zoneId: String, zoneName: String) =
        "$prefix/$zoneId?zoneName=${Uri.encode(zoneName)}"
    fun zoneDetail(zoneId: String, zoneName: String) = zoneScoped("zone", zoneId, zoneName)
    fun dns(zoneId: String, zoneName: String) = zoneScoped("dns", zoneId, zoneName)
    fun analytics(zoneId: String, zoneName: String) = zoneScoped("analytics", zoneId, zoneName)
    fun waf(zoneId: String, zoneName: String) = zoneScoped("waf", zoneId, zoneName)
    fun snippets(zoneId: String, zoneName: String) = zoneScoped("snippets", zoneId, zoneName)
    fun zoneSettings(zoneId: String, zoneName: String) = zoneScoped("zonesettings", zoneId, zoneName)
    fun ssl(zoneId: String, zoneName: String) = zoneScoped("ssl", zoneId, zoneName)
    fun sslCerts(zoneId: String, zoneName: String) = zoneScoped("sslcerts", zoneId, zoneName)
    fun performance(zoneId: String, zoneName: String) = zoneScoped("performance", zoneId, zoneName)
    fun transform(zoneId: String, zoneName: String) = zoneScoped("transform", zoneId, zoneName)
    fun accessRules(zoneId: String, zoneName: String) = zoneScoped("accessrules", zoneId, zoneName)
    fun snippetEdit(zoneId: String, zoneName: String, name: String) =
        "snippetEdit/$zoneId?zoneName=${Uri.encode(zoneName)}&name=${Uri.encode(name)}"
    fun identity(sessionId: String): String = "identity/${Uri.encode(sessionId)}"
    fun tunnelDetail(id: String, name: String): String = "tunnel/$id?tunnelName=${Uri.encode(name)}"
    fun worker(scriptName: String): String = "worker/${Uri.encode(scriptName)}"
    fun workerSecrets(scriptName: String): String = "worker/${Uri.encode(scriptName)}/secrets"
    fun workerTriggers(scriptName: String): String = "worker/${Uri.encode(scriptName)}/triggers"
    fun workerDomains(scriptName: String): String = "worker/${Uri.encode(scriptName)}/domains"
    fun tail(scriptName: String): String = "tail/${Uri.encode(scriptName)}"
    fun r2Objects(bucket: String): String = "r2/objects/${Uri.encode(bucket)}"
    fun r2Settings(bucket: String): String = "r2/settings/${Uri.encode(bucket)}"
    fun d1Query(dbId: String, dbName: String): String = "d1/query/$dbId?dbName=${Uri.encode(dbName)}"
    fun d1Table(dbId: String, table: String): String = "d1/table/$dbId?table=${Uri.encode(table)}"
    fun kvKeys(nsId: String, nsTitle: String): String = "kv/keys/$nsId?nsTitle=${Uri.encode(nsTitle)}"

    /** 路由 → 高亮的顶级标签（下钻页归属其父标签）。 */
    fun topOf(route: String?): TopDestination = when {
        route == DASHBOARD || route == TUNNELS || route?.startsWith("tunnel/") == true -> TopDestination.Dashboard
        route == SETTINGS || route == STATUS || route?.startsWith("identity/") == true -> TopDestination.Settings
        route == WORKERS || route?.startsWith("worker/") == true || route?.startsWith("tail/") == true ->
            TopDestination.Workers
        route == STORAGE || route?.startsWith("r2/") == true || route?.startsWith("d1/") == true || route?.startsWith("kv/") == true ->
            TopDestination.Storage
        else -> TopDestination.Zones
    }

    fun startRoute(dest: TopDestination): String = when (dest) {
        TopDestination.Dashboard -> DASHBOARD
        TopDestination.Zones -> ZONES
        TopDestination.Workers -> WORKERS
        TopDestination.Storage -> STORAGE
        TopDestination.Settings -> SETTINGS
    }
}

/** zone 级页面的共享导航参数（zoneId 必填 + 可选 zoneName）。 */
private fun zoneArgs() = listOf(
    navArgument("zoneId") { type = NavType.StringType },
    navArgument("zoneName") { type = NavType.StringType; defaultValue = "" },
)

@OptIn(ExperimentalMaterial3AdaptiveNavigationSuiteApi::class)
@Composable
private fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 下钻页仍高亮其父标签；付费墙是覆盖在来源页上的覆盖层、不归任何标签，
    // 故按来源页（上一条回退栈）高亮，避免落到 topOf 的 else 分支误高亮「域名」。
    val selectedTop =
        if (currentRoute == Dest.PAYWALL) Dest.topOf(navController.previousBackStackEntry?.destination?.route)
        else Dest.topOf(currentRoute)

    val whatsNewViewModel: WhatsNewViewModel = hiltViewModel()
    val whatsNewRelease by whatsNewViewModel.release.collectAsStateWithLifecycle()

    // 添加账号 = 无痕重新登录（第二账号走 Pro 闸门）
    val context = LocalContext.current
    val loginViewModel: LoginViewModel = hiltViewModel()
    val gateViewModel: ProGateViewModel = hiltViewModel()
    val isPro by gateViewModel.isPro.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        loginViewModel.launchAuthTab.collect { uri -> context.launchCustomTab(uri) }
    }
    val onAddAccount = {
        if (isPro) loginViewModel.login(freshLogin = true) else navController.navigate(Dest.PAYWALL)
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopDestination.entries.forEach { dest ->
                item(
                    selected = selectedTop == dest,
                    onClick = {
                        navController.navigate(Dest.startRoute(dest)) {
                            // 切顶级标签：弹回起点并保存/恢复各自子栈状态
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(dest.icon, contentDescription = null) },
                    label = { Text(stringResource(dest.labelRes)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = Dest.DASHBOARD) {
            composable(Dest.DASHBOARD) {
                DashboardScreen(
                    onOpenTunnels = { navController.navigate(Dest.TUNNELS) },
                    onOpenZones = {
                        navController.navigate(Dest.ZONES) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenZone = { zone -> navController.navigate(Dest.zoneDetail(zone.id, zone.name)) },
                    onAddAccount = onAddAccount,
                )
            }
            composable(Dest.PAYWALL) {
                PaywallScreen()
            }
            composable(Dest.TUNNELS) {
                ProGate {
                    TunnelListScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTunnel = { id, name -> navController.navigate(Dest.tunnelDetail(id, name)) },
                    )
                }
            }
            composable(
                route = Dest.TUNNEL_DETAIL_ROUTE,
                arguments = listOf(
                    navArgument("tunnelId") { type = NavType.StringType },
                    navArgument("tunnelName") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                ProGate { TunnelDetailScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                Dest.ZONES,
                deepLinks = listOf(navDeepLink { uriPattern = "orangecloud://open/zones" }),
            ) {
                ZonesPaneScreen(
                    onOpenDns = { id, name -> navController.navigate(Dest.dns(id, name)) },
                    onOpenAnalytics = { id, name -> navController.navigate(Dest.analytics(id, name)) },
                    onOpenWaf = { id, name -> navController.navigate(Dest.waf(id, name)) },
                    onOpenSnippets = { id, name -> navController.navigate(Dest.snippets(id, name)) },
                    onOpenSsl = { id, name -> navController.navigate(Dest.ssl(id, name)) },
                    onOpenSslCerts = { id, name -> navController.navigate(Dest.sslCerts(id, name)) },
                    onOpenTransform = { id, name -> navController.navigate(Dest.transform(id, name)) },
                    onOpenAccessRules = { id, name -> navController.navigate(Dest.accessRules(id, name)) },
                    onOpenPerformance = { id, name -> navController.navigate(Dest.performance(id, name)) },
                    onOpenSettings = { id, name -> navController.navigate(Dest.zoneSettings(id, name)) },
                )
            }
            composable(
                route = Dest.ZONE_DETAIL_ROUTE,
                arguments = zoneArgs(),
            ) { entry ->
                val zoneId = entry.arguments?.getString("zoneId").orEmpty()
                val zoneName = entry.arguments?.getString("zoneName").orEmpty()
                ZoneDetailScreen(
                    zoneId = zoneId,
                    zoneName = zoneName,
                    onBack = { navController.popBackStack() },
                    onOpenDns = { navController.navigate(Dest.dns(zoneId, zoneName)) },
                    onOpenAnalytics = { navController.navigate(Dest.analytics(zoneId, zoneName)) },
                    onOpenWaf = { navController.navigate(Dest.waf(zoneId, zoneName)) },
                    onOpenSnippets = { navController.navigate(Dest.snippets(zoneId, zoneName)) },
                    onOpenSsl = { navController.navigate(Dest.ssl(zoneId, zoneName)) },
                    onOpenSslCerts = { navController.navigate(Dest.sslCerts(zoneId, zoneName)) },
                    onOpenTransform = { navController.navigate(Dest.transform(zoneId, zoneName)) },
                    onOpenAccessRules = { navController.navigate(Dest.accessRules(zoneId, zoneName)) },
                    onOpenPerformance = { navController.navigate(Dest.performance(zoneId, zoneName)) },
                    onOpenSettings = { navController.navigate(Dest.zoneSettings(zoneId, zoneName)) },
                )
            }
            composable(
                route = Dest.WAF_ROUTE,
                arguments = zoneArgs(),
            ) {
                ProGate { WafRulesScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                route = Dest.SNIPPETS_ROUTE,
                arguments = zoneArgs(),
            ) { entry ->
                val zoneId = entry.arguments?.getString("zoneId").orEmpty()
                val zoneName = entry.arguments?.getString("zoneName").orEmpty()
                ProGate {
                    SnippetsListScreen(
                        onBack = { navController.popBackStack() },
                        onOpenSnippet = { name -> navController.navigate(Dest.snippetEdit(zoneId, zoneName, name)) },
                        onCreate = { navController.navigate(Dest.snippetEdit(zoneId, zoneName, "")) },
                    )
                }
            }
            composable(
                route = Dest.SNIPPET_EDIT_ROUTE,
                arguments = listOf(
                    navArgument("zoneId") { type = NavType.StringType },
                    navArgument("zoneName") { type = NavType.StringType; defaultValue = "" },
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                ProGate {
                    SnippetEditorScreen(
                        onBack = { navController.popBackStack() },
                        onClosed = { navController.popBackStack() },
                    )
                }
            }
            composable(
                route = Dest.ZONE_SETTINGS_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.SSL_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneSslSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.SSL_CERTS_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneSslCertsScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.PERFORMANCE_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZonePerformanceScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.TRANSFORM_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneTransformScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.ACCESS_RULES_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneAccessRulesScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.DNS_ROUTE,
                arguments = zoneArgs(),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "orangecloud://zone/{zoneId}/dns?zoneName={zoneName}" },
                ),
            ) {
                DnsListScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Dest.ANALYTICS_ROUTE,
                arguments = zoneArgs(),
            ) {
                ZoneAnalyticsScreen(
                    onBack = { navController.popBackStack() },
                    onShowPaywall = { navController.navigate(Dest.PAYWALL) },
                )
            }
            composable(
                Dest.WORKERS,
                deepLinks = listOf(navDeepLink { uriPattern = "orangecloud://open/workers" }),
            ) {
                WorkerListScreen(
                    onWorkerClick = { name -> navController.navigate(Dest.worker(name)) },
                )
            }
            composable(
                route = Dest.WORKER_ROUTE,
                arguments = listOf(navArgument("scriptName") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "orangecloud://worker/{scriptName}" },
                ),
            ) { entry ->
                val scriptName = entry.arguments?.getString("scriptName").orEmpty()
                WorkerDetailScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTail = { navController.navigate(Dest.tail(scriptName)) },
                    onOpenSecrets = { navController.navigate(Dest.workerSecrets(scriptName)) },
                    onOpenTriggers = { navController.navigate(Dest.workerTriggers(scriptName)) },
                    onOpenDomains = { navController.navigate(Dest.workerDomains(scriptName)) },
                )
            }
            composable(
                route = Dest.WORKER_SECRETS_ROUTE,
                arguments = listOf(navArgument("scriptName") { type = NavType.StringType }),
            ) {
                ProGate { WorkerSecretsScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                route = Dest.WORKER_TRIGGERS_ROUTE,
                arguments = listOf(navArgument("scriptName") { type = NavType.StringType }),
            ) {
                ProGate { WorkerTriggersScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                route = Dest.WORKER_DOMAINS_ROUTE,
                arguments = listOf(navArgument("scriptName") { type = NavType.StringType }),
            ) {
                ProGate { WorkerRoutesScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                route = Dest.TAIL_ROUTE,
                arguments = listOf(navArgument("scriptName") { type = NavType.StringType }),
            ) {
                ProGate { WorkerTailScreen(onBack = { navController.popBackStack() }) }
            }
            composable(
                Dest.STORAGE,
                deepLinks = listOf(navDeepLink { uriPattern = "orangecloud://open/storage" }),
            ) {
                ProGate {
                    StorageHubScreen(
                        onOpenR2 = { navController.navigate(Dest.R2_BUCKETS) },
                        onOpenD1 = { navController.navigate(Dest.D1_DATABASES) },
                        onOpenKV = { navController.navigate(Dest.KV_NAMESPACES) },
                    )
                }
            }
            composable(Dest.R2_BUCKETS) {
                R2BucketListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBucket = { navController.navigate(Dest.r2Objects(it)) },
                )
            }
            composable(
                route = Dest.R2_OBJECTS_ROUTE,
                arguments = listOf(navArgument("bucket") { type = NavType.StringType }),
            ) { entry ->
                val bucket = entry.arguments?.getString("bucket").orEmpty()
                R2ObjectListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenSettings = { navController.navigate(Dest.r2Settings(bucket)) },
                )
            }
            composable(
                route = Dest.R2_BUCKET_SETTINGS_ROUTE,
                arguments = listOf(navArgument("bucket") { type = NavType.StringType }),
            ) {
                R2BucketSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.D1_DATABASES) {
                D1DatabaseListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDatabase = { id, name -> navController.navigate(Dest.d1Query(id, name)) },
                )
            }
            composable(
                route = Dest.D1_QUERY_ROUTE,
                arguments = listOf(
                    navArgument("dbId") { type = NavType.StringType },
                    navArgument("dbName") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val dbId = entry.arguments?.getString("dbId").orEmpty()
                D1QueryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenTable = { table -> navController.navigate(Dest.d1Table(dbId, table)) },
                )
            }
            composable(
                route = Dest.D1_TABLE_ROUTE,
                arguments = listOf(
                    navArgument("dbId") { type = NavType.StringType },
                    navArgument("table") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                D1TableScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.KV_NAMESPACES) {
                KVNamespaceListScreen(
                    onBack = { navController.popBackStack() },
                    onOpenNamespace = { id, title -> navController.navigate(Dest.kvKeys(id, title)) },
                )
            }
            composable(
                route = Dest.KV_KEYS_ROUTE,
                arguments = listOf(
                    navArgument("nsId") { type = NavType.StringType },
                    navArgument("nsTitle") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                KVKeyListScreen(onBack = { navController.popBackStack() })
            }
            composable(Dest.SETTINGS) {
                SettingsScreen(
                    onOpenStatus = { navController.navigate(Dest.STATUS) },
                    onOpenIdentity = { sessionId -> navController.navigate(Dest.identity(sessionId)) },
                    onAddAccount = onAddAccount,
                    onOpenPaywall = { navController.navigate(Dest.PAYWALL) },
                )
            }
            composable(
                route = Dest.IDENTITY_ROUTE,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType }),
            ) { entry ->
                val sessionId = entry.arguments?.getString("sessionId").orEmpty()
                IdentityDetailScreen(
                    sessionId = sessionId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Dest.STATUS) {
                StatusScreen(onBack = { navController.popBackStack() })
            }
        }
    }

    whatsNewRelease?.let { WhatsNewDialog(it, whatsNewViewModel::dismiss) }
}

@Composable
private fun SplashScreen() {
    val isDark = jiamin.chen.orangecloud.core.design.theme.LocalIsDark.current
    val phase = remember(isDark) { SkyPhase.current(isDark, LocalTime.now().hour) }
    SkyBackground(phase = phase) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = if (phase.isDark) Color.White else MaterialTheme.colorScheme.primary,
            )
        }
    }
}
