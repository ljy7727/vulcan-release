package com.heda.vulcan.data

import java.io.File

/**
 * 桌面版偏好设置（等价于电脑版的 prefs.json）。
 * 存放于 %USERPROFILE%\.vulcan-desktop\prefs.json。
 */
class DesktopPrefs(private val file: File = File(DesktopDb.dataDir(), "prefs.json")) {

    var extraSeconds: Int = 14          // 机动时间（秒）
    var themeId: String = "orange"      // 主题
    var darkMode: Boolean = false
    var searchTrigger: String = "4space" // 全局搜索触发（四击空格）
    var screenshotHotkey: String = "alt Z"
    var closeToTray: Boolean = true
    var trayEnabled: Boolean = true
    var iconPath: String = ""
    var bgPath: String = ""
    var exportDir: String = ""
    var machines: MutableList<String> = MachineList.machineIds.toMutableList()
    /** 已激活的版本号（与 AppVersion.VERSION 相同即视为本版本已激活） */
    var activatedVersion: String = ""
    /** 上次检查更新的时间（毫秒） */
    var lastUpdateCheck: Long = 0L

    init { load() }

    fun load() {
        if (!file.exists()) return
        try {
            val root = Json.parse(file.readText()).asMap()
            extraSeconds = root["extra_seconds"].asIntOr(14)
            themeId = root["theme_id"].asStr().ifEmpty { "orange" }
            darkMode = root["dark_mode"] == true
            searchTrigger = root["search_trigger"].asStr().ifEmpty { "4space" }
            screenshotHotkey = root["screenshot_hotkey"].asStr().ifEmpty { "alt Z" }
            closeToTray = root["close_to_tray"] != false
            trayEnabled = root["tray_enabled"] != false
            iconPath = root["icon_path"].asStr()
            bgPath = root["bg_path"].asStr()
            exportDir = root["export_dir"].asStr()
            val m = root["machines"].asList().map { it.asStr() }.filter { it.isNotEmpty() }
            if (m.isNotEmpty()) machines = m.toMutableList()
            activatedVersion = root["activated_version"].asStr()
            lastUpdateCheck = (root["last_update_check"] as? Number)?.toLong() ?: 0L
        } catch (_: Exception) {
            // 解析失败则使用默认值
        }
    }

    fun save() {
        file.parentFile?.mkdirs()
        val root = linkedMapOf<String, Any?>(
            "search_trigger" to searchTrigger,
            "close_to_tray" to closeToTray,
            "dark_mode" to darkMode,
            "icon_path" to iconPath,
            "extra_seconds" to extraSeconds,
            "theme_id" to themeId,
            "tray_menu_ascii" to false,
            "tray_enabled" to trayEnabled,
            "bg_path" to bgPath,
            "machines" to machines,
            "screenshot_hotkey" to screenshotHotkey,
            "export_dir" to exportDir,
            "activated_version" to activatedVersion,
            "last_update_check" to lastUpdateCheck
        )
        file.writeText(Json.write(root))
    }
}
