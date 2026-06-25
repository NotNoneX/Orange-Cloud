package jiamin.chen.orangecloud.data.repository

import jiamin.chen.orangecloud.core.network.CfApiClient
import jiamin.chen.orangecloud.data.model.PurgeFilesRequest
import jiamin.chen.orangecloud.data.model.PurgeRequest
import jiamin.chen.orangecloud.data.model.PurgeResult
import jiamin.chen.orangecloud.data.model.ZoneSetting
import jiamin.chen.orangecloud.data.model.ZoneSettingUpdate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zone 设置读写（development_mode / security_level）+ 全量缓存清理（对应 iOS ZoneSettingsService）。
 */
@Singleton
class ZoneSettingsRepository @Inject constructor(
    private val api: CfApiClient,
) {
    suspend fun getSetting(zoneId: String, setting: String): String =
        api.get<ZoneSetting>("zones/$zoneId/settings/$setting").value

    suspend fun setSetting(zoneId: String, setting: String, value: String): String =
        api.patch<ZoneSetting, ZoneSettingUpdate>("zones/$zoneId/settings/$setting", ZoneSettingUpdate(value)).value

    suspend fun purgeAllCache(zoneId: String) {
        api.post<PurgeResult, PurgeRequest>("zones/$zoneId/purge_cache", PurgeRequest(purgeEverything = true))
    }

    /** 按 URL 清理缓存（单文件 purge，单次最多 30 个 URL）。 */
    suspend fun purgeFiles(zoneId: String, urls: List<String>) {
        api.post<PurgeResult, PurgeFilesRequest>("zones/$zoneId/purge_cache", PurgeFilesRequest(files = urls))
    }
}
