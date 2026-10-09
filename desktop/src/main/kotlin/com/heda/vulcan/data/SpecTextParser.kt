package com.heda.vulcan.data

import java.util.Locale

/**
 * 规格文本解析（从剪贴板文字自动识别 花纹/规格/区域/机台）。
 * 与原版 com.heda.vulcan.data.SpecTextParser 逻辑一致（反编译还原）。
 *
 * 例："225/50ZR17 XL RU06 4 98W CH LB DURUN"
 *   → size=225/50ZR17, region=CH, pattern=RU06, machine=null
 * 例："165R13C C212 8 94/93R CN LB DURUN"
 *   → size=165R13C, region=CN, pattern=C212
 */
object SpecTextParser {

    /** 规格：225/50ZR17、165R13C、225R19LT 等 */
    private val sizeRegex = Regex(
        """(?:\d{2,3}/\d{2}[A-Z]?R\d{2}(?![0-9])|\d{3}R\d{2}LT|\d{3}R\d{2}C)""",
        RegexOption.IGNORE_CASE
    )

    /** 花纹形如 RU06 / BC7778 / S701 / YS82D */
    private val patternLike = Regex("""^[A-Z]{1,5}\d{2,4}[A-Z]?$""")

    private val pureNumber = Regex("""^\d{1,3}$""")

    /** 载重指数 98W / 94V */
    private val loadIndex = Regex("""^\d{2,3}[A-Z]$""")

    private val regionSet = setOf("CH", "EU", "CN")

    private val brandWords = setOf(
        "KAPSEN", "HABILEAD", "HEDA", "LANVIGATOR", "TIRE", "TYRE",
        "PCR", "TBR", "LB", "XL", "TL", "TT", "RFT", "MFS"
    )

    data class Parsed(
        val raw: String,
        val pattern: String?,
        val size: String,
        val region: String?,
        val machine: String?
    ) {
        /** 花纹+规格齐备即可用于查询 */
        val usable: Boolean
            get() = !pattern.isNullOrBlank() && size.isNotEmpty()
    }

    fun parse(text: String, knownPatterns: List<String> = emptyList()): Parsed {
        val up = text.uppercase(Locale.ROOT)
        val size = sizeRegex.find(up)?.value?.uppercase(Locale.ROOT) ?: ""
        val tokens = Regex("[^A-Z0-9/]+").split(up).filter { it.isNotEmpty() }
        val region = tokens.lastOrNull { it in regionSet }
        val machine = tokens.firstOrNull { it in MachineList.machineIds }
        val known = knownPatterns.map { it.trim().uppercase(Locale.ROOT) }.filter { it.isNotEmpty() }

        val candidates = tokens.filter { t ->
            t != size && t !in regionSet && t != machine && t !in brandWords &&
                    !pureNumber.matches(t) && !loadIndex.matches(t) && !t.contains("/")
        }

        val pattern = candidates.firstOrNull { it in known }
            ?: candidates.firstOrNull { c -> known.any { c.contains(it) || it.contains(c) } }
            ?: candidates.firstOrNull { patternLike.matches(it) }
            ?: candidates.firstOrNull()

        return Parsed(text, pattern, size, region, machine)
    }

    fun describe(p: Parsed): String {
        val machinePart = p.machine?.let { " · 机台 $it" } ?: ""
        return "已自动识别：花纹 ${p.pattern ?: "?"} · 规格 ${p.size.ifEmpty { "?" }} · 区域 ${p.region ?: "全部"}$machinePart"
    }
}
