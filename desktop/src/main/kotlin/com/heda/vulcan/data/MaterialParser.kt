package com.heda.vulcan.data

/**
 * 物料字符串解析工具。
 * 物料示例：225/50ZR17 XL RU06 4 98W CH LB DURUN
 * 尺寸 = 第一个词；花纹 = 已知 sheet 名中的词；区域 = CH/EU/CN 词。
 */
object MaterialParser {

    private val regionSet = setOf("CH", "EU", "CN")

    fun tokens(material: String): List<String> =
        material.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    /** 尺寸 = 第一个词 */
    fun extractSize(material: String): String =
        tokens(material).firstOrNull() ?: ""

    /** 区域：CH/EU/CN（若无则 null） */
    fun extractRegion(material: String): String? =
        tokens(material).firstOrNull { it in regionSet }

    /** 花纹：优先取等于已知 sheet 名的词 */
    fun extractPattern(material: String, knownPatterns: List<String>): String? {
        val tk = tokens(material)
        val known = knownPatterns.map { it.trim().uppercase() }.toSet()
        return tk.firstOrNull { it.uppercase() in known }
    }

    /** 从完整物料中智能提取（尺寸、花纹、区域），花纹需要在 knownPatterns 中 */
    fun extractFromFull(material: String, knownPatterns: List<String>): Triple<String?, String?, String?> {
        val size = extractSize(material)
        val region = extractRegion(material)
        val pattern = extractPattern(material, knownPatterns)
        return Triple(size.ifEmpty { null }, pattern, region)
    }
}
