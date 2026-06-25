package jiamin.chen.orangecloud

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import jiamin.chen.orangecloud.core.logging.AppLog
import jiamin.chen.orangecloud.core.logging.LogFileStore

@HiltAndroidApp
class OrangeCloudApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 统一日志门面文件落地（反馈附件用，仅缓存目录、不含任何令牌/密钥）。
        AppLog.install(LogFileStore(cacheDir))
        AppLog.app.info("App start ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
    }
}
