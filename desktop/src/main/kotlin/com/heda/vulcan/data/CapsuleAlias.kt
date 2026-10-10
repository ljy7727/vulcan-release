package com.heda.vulcan.data

/**
 * 胶囊对齐（花纹别名映射）。
 *
 * 场景：规格里的花纹名与实际花纹不一致（如规格写 ZDT888，实际花纹是 HG918）。
 * 配置对齐关系后，识别/查询时遇到"不认识的花纹"会查此表换算，
 * 并在界面上注明「ZDT888 对应 HG918」。
 *
 * 全部**忽略大小写**（关键词与目标花纹均按大写比较）。
 */
object CapsuleAlias {

    /** 默认三条（用户在设置中可增删改） */
    fun defaultAliases(): List<List<String>> = listOf(
        listOf("STEADY-33", "A2000"),
        listOf("DRB01", "HG918"),
        listOf("DRB02", "RU06")
    )

    /** 把配置列表转成「大写关键词 → 目标花纹」的查找表（忽略大小写）。 */
    fun toLookup(aliases: List<List<String>>): Map<String, String> {
        val out = mutableMapOf<String, String>()
        for (pair in aliases) {
            if (pair.size >= 2) {
                val k = pair[0].trim().uppercase()
                val v = pair[1].trim()
                if (k.isNotEmpty() && v.isNotEmpty()) out[k] = v
            }
        }
        return out
    }

    /** 显示用：命中别名则追加「（花纹）」 */
    fun display(capsule: String, aliasLookup: Map<String, String>): String {
        val c = capsule.trim()
        if (c.isEmpty()) return c
        val pat = aliasLookup[c.uppercase()] ?: return c
        return "$c（$pat）"
    }

    /** 从任意文本里推断花纹（用于 OCR / 文本兜底） */
    fun resolveUnknown(text: String, aliasLookup: Map<String, String>): String? {
        for ((code, pat) in aliasLookup) {
            if (text.contains(code, ignoreCase = true)) return pat
        }
        return null
    }

    /**
     * 把"不认识的花纹"按对齐表换算为已知花纹。
     * @return 换算后的已知花纹；无对应关系时返回 null。
     */
    fun resolvePattern(pattern: String, knownPatterns: List<String>, aliasLookup: Map<String, String>): String? {
        val p = pattern.trim()
        if (p.isEmpty()) return null
        // 已在已知库中，无需换算
        if (knownPatterns.any { it.equals(p, ignoreCase = true) }) return null
        // 查对齐表（忽略大小写）
        val target = aliasLookup[p.uppercase()] ?: return null
        // 目标必须是已知花纹才可用
        return knownPatterns.firstOrNull { it.equals(target, ignoreCase = true) }
    }
}
