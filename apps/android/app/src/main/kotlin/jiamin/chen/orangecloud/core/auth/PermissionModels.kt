package jiamin.chen.orangecloud.core.auth

import jiamin.chen.orangecloud.R

/**
 * 可按功能勾选的权限项（对应 iOS PermissionModels.allFeatures）。
 * 勾选即纳入该功能的 read + edit scope；required 功能强制勾选。
 */
data class PermissionFeature(
    val id: String,
    val nameRes: Int,
    val descRes: Int,
    val readScopes: List<String>,
    val editScopes: List<String> = emptyList(),
    val required: Boolean = false,
)

object PermissionCatalog {
    val features: List<PermissionFeature> = listOf(
        PermissionFeature("account", R.string.perm_account, R.string.perm_account_desc, listOf(Scopes.ACCOUNT_READ)),
        PermissionFeature("zones", R.string.perm_zones, R.string.perm_zones_desc, listOf(Scopes.ZONE_READ), listOf(Scopes.ZONE_WRITE), required = true),
        PermissionFeature("dns", R.string.perm_dns, R.string.perm_dns_desc, listOf(Scopes.DNS_READ), listOf(Scopes.DNS_WRITE)),
        PermissionFeature("workers", R.string.perm_workers, R.string.perm_workers_desc, listOf(Scopes.WORKERS_READ, Scopes.WORKERS_ROUTES_READ), listOf(Scopes.WORKERS_WRITE, Scopes.WORKERS_ROUTES_WRITE)),
        PermissionFeature("workers_tail", R.string.perm_tail, R.string.perm_tail_desc, listOf(Scopes.WORKERS_TAIL_READ)),
        PermissionFeature("snippets", R.string.perm_snippets, R.string.perm_snippets_desc, listOf(Scopes.SNIPPETS_READ), listOf(Scopes.SNIPPETS_WRITE)),
        PermissionFeature("r2", R.string.perm_r2, R.string.perm_r2_desc, listOf(Scopes.R2_READ), listOf(Scopes.R2_WRITE)),
        PermissionFeature("d1", R.string.perm_d1, R.string.perm_d1_desc, listOf(Scopes.D1_READ), listOf(Scopes.D1_WRITE)),
        PermissionFeature("kv", R.string.perm_kv, R.string.perm_kv_desc, listOf(Scopes.KV_READ), listOf(Scopes.KV_WRITE)),
        PermissionFeature("tunnels", R.string.perm_tunnels, R.string.perm_tunnels_desc, listOf(Scopes.TUNNEL_READ), listOf(Scopes.TUNNEL_WRITE)),
        PermissionFeature("waf", R.string.perm_waf, R.string.perm_waf_desc, listOf(Scopes.WAF_READ), listOf(Scopes.WAF_WRITE)),
        PermissionFeature("zone_settings", R.string.perm_zone_settings, R.string.perm_zone_settings_desc, listOf(Scopes.ZONE_SETTINGS_READ), listOf(Scopes.ZONE_SETTINGS_WRITE, Scopes.CACHE_PURGE)),
        PermissionFeature("ssl_certs", R.string.perm_ssl_certs, R.string.perm_ssl_certs_desc, listOf(Scopes.SSL_CERTS_READ), listOf(Scopes.SSL_CERTS_WRITE)),
        PermissionFeature("transform_rules", R.string.perm_transform, R.string.perm_transform_desc, listOf(Scopes.TRANSFORM_READ), listOf(Scopes.TRANSFORM_WRITE)),
        PermissionFeature("ip_access_rules", R.string.perm_ip_rules, R.string.perm_ip_rules_desc, listOf(Scopes.FIREWALL_READ), listOf(Scopes.FIREWALL_WRITE)),
        PermissionFeature("analytics", R.string.perm_analytics, R.string.perm_analytics_desc, listOf(Scopes.ACCOUNT_ANALYTICS_READ, Scopes.ANALYTICS_READ)),
    )

    /** 默认全选的功能 id 集合。 */
    val defaultSelectedIds: Set<String> = features.map { it.id }.toSet()

    /** 由勾选的功能 id 计算 scope 字符串（read + edit，排序去重，空格分隔）。 */
    fun scopeString(selectedIds: Set<String>): String {
        val scopes = sortedSetOf<String>()
        for (feature in features) {
            if (feature.required || feature.id in selectedIds) {
                scopes += feature.readScopes
                scopes += feature.editScopes
            }
        }
        return scopes.joinToString(" ")
    }

    /**
     * 按访问级别计算 scope：levels[id] = true 表示读写（read + edit），false 表示只读（仅 read）。
     * 未在 map 中的功能不申请；required 功能强制至少只读。
     */
    fun scopeString(levels: Map<String, Boolean>): String {
        val scopes = sortedSetOf<String>()
        for (feature in features) {
            val included = feature.required || feature.id in levels
            if (!included) continue
            scopes += feature.readScopes
            if (levels[feature.id] == true) scopes += feature.editScopes
        }
        return scopes.joinToString(" ")
    }
}
