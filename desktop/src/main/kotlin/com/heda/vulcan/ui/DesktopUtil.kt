package com.heda.vulcan.ui

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/** 桌面工具（剪贴板等），对应原版 ui/Util 的剪贴板部分。 */
object DesktopUtil {

    /** 读系统剪贴板文本；无文本或异常返回 null。 */
    fun clipboardText(): String? = try {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
            (cb.getData(DataFlavor.stringFlavor) as? String)?.trim()?.takeIf { it.isNotEmpty() }
        } else null
    } catch (e: Exception) {
        null
    }

    /** 写文本到剪贴板。 */
    fun copyText(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
