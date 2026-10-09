package com.heda.vulcan.core

import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Window
import java.awt.image.BufferedImage

private var trayIcon: TrayIcon? = null

/**
 * 系统托盘（等价于电脑版的 TrayWatch）。
 * 右键弹中文菜单：打开主窗口 / 退出程序；双击打开主窗口。
 */
fun setupTray(window: Window, enabled: Boolean, onExit: () -> Unit, onShow: () -> Unit) {
    if (!enabled) return
    if (!SystemTray.isSupported()) {
        AppLog.line("当前系统不支持托盘")
        return
    }
    if (trayIcon != null) return
    try {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color(0x15, 0x65, 0xC0)
        g.fillRect(0, 0, 16, 16)
        g.dispose()

        val popup = PopupMenu()
        val itemShow = MenuItem("打开主窗口")
        itemShow.addActionListener { onShow() }
        val itemExit = MenuItem("退出程序 (Exit)")
        itemExit.addActionListener { onExit() }
        popup.add(itemShow)
        popup.add(itemExit)

        val icon = TrayIcon(image, "硫化工艺助手", popup)
        icon.addActionListener { onShow() }
        SystemTray.getSystemTray().add(icon)
        trayIcon = icon
        AppLog.line("托盘图标已添加")
    } catch (e: Throwable) {
        AppLog.line("托盘初始化失败: ${e.message}")
    }
}
