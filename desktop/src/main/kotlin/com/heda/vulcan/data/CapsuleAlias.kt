package com.heda.vulcan.data

/**
 * 胶囊别名/显示增强（桌面版特有，来自反编译还原）。
 * DRB01 → DRB01（HG918），DRB02 → DRB02（RU06）。
 */
object CapsuleAlias {

    private val map: Map<String, String> = mapOf(
        "DRB01" to "HG918",
        "DRB02" to "RU06"
    )

    /** 显示用：命中别名则追加"（花纹）" */
    fun display(capsule: String): String {
        val c = capsule.trim()
        if (c.isEmpty()) return c
        val pat = map[c.uppercase()] ?: return c
        return "$c（$pat）"
    }

    /** 从任意文本里推断花纹（用于 OCR 文本兜底） */
    fun resolveUnknown(text: String): String? {
        for ((code, pat) in map) {
            if (text.contains(code, ignoreCase = true)) return pat
        }
        return null
    }
}
