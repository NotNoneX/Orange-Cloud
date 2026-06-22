//
//  WhatsNewReleases.generated.swift
//  Orange Cloud
//
//  ⚠️ 自动生成 —— 请勿手改。改 packages/changelog/ios.json 后运行 `pnpm changelog:gen`。
//  字符串走 WhatsNew.xcstrings（table: "WhatsNew"），与 Localizable.xcstrings 解耦。
//

import Foundation

nonisolated enum WhatsNewGenerated {
    static let releases: [WhatsNewRelease] = [
        WhatsNewRelease(version: "1.3.2", items: [
            WhatsNewItem(
                icon:   "sparkles",
                title:  String(localized: "用自然语言写 WAF 规则", table: "WhatsNew"),
                detail: String(localized: "在支持 Apple 智能的设备上，用一句话描述需求即可生成 WAF 自定义规则，也能把现有规则翻译成大白话；全程在设备上离线完成。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "wrench.and.screwdriver",
                title:  String(localized: "修复启动闪退", table: "WhatsNew"),
                detail: String(localized: "修复了 App 在部分 iOS 17 设备上一启动就闪退的问题。", table: "WhatsNew")
            )
        ]),
        WhatsNewRelease(version: "1.3.0", items: [
            WhatsNewItem(
                icon:   "globe.badge.chevron.right",
                title:  String(localized: "添加域名", table: "WhatsNew"),
                detail: String(localized: "在 App 里把已注册的域名加入账号，并拿到要去注册商处配置的名称服务器。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "network",
                title:  String(localized: "Tunnel 管理", table: "WhatsNew"),
                detail: String(localized: "不再只是查看——新建隧道、获取连接令牌与命令、配置公共主机名路由。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "cylinder.split.1x2",
                title:  String(localized: "D1 数据库管理", table: "WhatsNew"),
                detail: String(localized: "直接新建 D1 数据库，或在原样确认库名后安全删除。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "key",
                title:  String(localized: "变量与密钥", table: "WhatsNew"),
                detail: String(localized: "管理 Worker 的环境变量与密钥，随手增删改。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "clock",
                title:  String(localized: "定时触发器", table: "WhatsNew"),
                detail: String(localized: "查看与增删 Cron 触发器，让 Worker 按计划自动运行。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "globe",
                title:  String(localized: "域名与路由", table: "WhatsNew"),
                detail: String(localized: "管理 workers.dev 子域、自定义域与路由，掌控 Worker 的访问入口。", table: "WhatsNew")
            )
        ]),
        WhatsNewRelease(version: "1.2.1", items: [
            WhatsNewItem(
                icon:   "applewatch",
                title:  String(localized: "Apple Watch App", table: "WhatsNew"),
                detail: String(localized: "把域名状态与流量概览带上手腕，还能添加到表盘复杂功能随时一瞥。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "curlybraces",
                title:  String(localized: "Snippets", table: "WhatsNew"),
                detail: String(localized: "在域名详情查看、编辑、新建 Cloudflare 边缘代码片段，并管理触发规则——轻量版 Workers，Pro 解锁。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "accessibility",
                title:  String(localized: "全面无障碍", table: "WhatsNew"),
                detail: String(localized: "VoiceOver、更大字体、不只靠颜色区分、足够对比度全面达标，配合系统辅助功能更顺手。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "character.bubble",
                title:  String(localized: "更多语言", table: "WhatsNew"),
                detail: String(localized: "新增西班牙语、韩语、葡萄牙语，现已支持九种语言。", table: "WhatsNew")
            ),
            WhatsNewItem(
                icon:   "arrow.clockwise",
                title:  String(localized: "刷新更省心", table: "WhatsNew"),
                detail: String(localized: "刷新失败不再弹窗打断，下拉刷新更稳定可靠。", table: "WhatsNew")
            )
        ])
    ]
}
