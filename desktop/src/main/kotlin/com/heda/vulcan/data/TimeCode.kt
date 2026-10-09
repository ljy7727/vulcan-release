package com.heda.vulcan.data

/**
 * 硫化时间代码解码。
 * 格式：PB + 温度(3位) + 字母(Q/H/T/Z) + 分钟数
 * 例：PB178T10 → 模套温度178℃，T=45秒，10分钟 → 10分45秒（再加机动时间）
 * 字母：Q=15秒，H=30秒，T=45秒，Z=0秒
 */
data class TimeCodeResult(
    val raw: String,
    val temp: Int?,          // 模套温度 ℃
    val letter: Char?,       // Q/H/T/Z
    val letterSec: Int,      // 字母对应秒数
    val minutes: Int,        // 代码中的分钟数
    val baseMinutes: Int,    // 分钟数
    val baseSeconds: Int,    // 分钟+字母秒
    val extraSeconds: Int,   // 机动时间
    val finalMinutes: Int,   // 最终硫化时间
    val finalSeconds: Int
) {
    val baseText: String get() = formatMinSec(baseMinutes, baseSeconds)
    val finalText: String get() = formatMinSec(finalMinutes, finalSeconds)

    companion object {
        fun formatMinSec(m: Int, s: Int): String {
            val mm = m + s / 60
            val ss = s % 60
            return if (ss == 0) "${mm}分" else "${mm}分${ss}秒"
        }
    }
}

object TimeCode {

    private val regex = Regex("^[A-Za-z]{0,2}(\\d{3})?([QHTZqhtz])(\\d{1,2})$")

    /** 解析时间代码；extraSeconds 为机动时间（默认 14 秒）。解析失败返回 null。 */
    fun parse(code: String, extraSeconds: Int = 14): TimeCodeResult? {
        val c = code.trim().uppercase()
        if (c.isEmpty()) return null
        val m = regex.find(c) ?: return null
        val temp = m.groupValues[1].takeIf { it.isNotEmpty() }?.toIntOrNull()
        val letter = m.groupValues[2][0]
        val mins = m.groupValues[3].toIntOrNull() ?: return null
        val sec = when (letter) {
            'Q' -> 15
            'H' -> 30
            'T' -> 45
            else -> 0 // Z
        }
        val baseSec = mins * 60 + sec
        val finalSec = baseSec + extraSeconds
        return TimeCodeResult(
            raw = c,
            temp = temp,
            letter = letter,
            letterSec = sec,
            minutes = mins,
            baseMinutes = finalSec / 60,
            baseSeconds = finalSec % 60,
            extraSeconds = extraSeconds,
            finalMinutes = finalSec / 60,
            finalSeconds = finalSec % 60
        )
    }

    fun letterLabel(l: Char?): String = when (l) {
        'Q' -> "Q=15秒"
        'H' -> "H=30秒"
        'T' -> "T=45秒"
        'Z' -> "Z=0秒"
        else -> ""
    }
}
