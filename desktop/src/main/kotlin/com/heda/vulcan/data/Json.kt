package com.heda.vulcan.data

/**
 * 极简 JSON 读写（零依赖）。
 * 用途：① 偏好设置持久化；② 三端互通的 JSON 数据包 导入/导出。
 * 支持：对象、数组、字符串、数字、布尔、null。
 */
object Json {

    // ---------- 写 ----------
    fun write(v: Any?): String {
        val sb = StringBuilder()
        writeValue(sb, v)
        return sb.toString()
    }

    private fun writeValue(sb: StringBuilder, v: Any?) {
        when (v) {
            null -> sb.append("null")
            is String -> writeString(sb, v)
            is Boolean -> sb.append(v.toString())
            is Int, is Long -> sb.append(v.toString())
            is Double -> sb.append(if (v == v.toLong().toDouble()) v.toLong().toString() else v.toString())
            is Float -> writeValue(sb, v.toDouble())
            is Map<*, *> -> {
                sb.append('{')
                var first = true
                for ((k, value) in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeString(sb, k.toString())
                    sb.append(':')
                    writeValue(sb, value)
                }
                sb.append('}')
            }
            is Iterable<*> -> {
                sb.append('[')
                var first = true
                for (item in v) {
                    if (!first) sb.append(',')
                    first = false
                    writeValue(sb, item)
                }
                sb.append(']')
            }
            else -> writeString(sb, v.toString())
        }
    }

    private fun writeString(sb: StringBuilder, s: String) {
        sb.append('"')
        for (ch in s) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (ch < ' ') sb.append("\\u%04x".format(ch.code)) else sb.append(ch)
            }
        }
        sb.append('"')
    }

    // ---------- 读 ----------
    fun parse(text: String): Any? = Parser(text).parseValue()

    private class Parser(private val s: String) {
        private var i = 0

        fun parseValue(): Any? {
            skipWs()
            if (i >= s.length) return null
            return when (s[i]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't', 'f' -> parseBool()
                'n' -> { i += 4; null }
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++ // {
            skipWs()
            if (i < s.length && s[i] == '}') { i++; return m }
            while (i < s.length) {
                skipWs()
                val key = parseString()
                skipWs()
                if (i < s.length && s[i] == ':') i++
                val value = parseValue()
                m[key] = value
                skipWs()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == '}') { i++; break }
                break
            }
            return m
        }

        private fun parseArray(): List<Any?> {
            val l = mutableListOf<Any?>()
            i++ // [
            skipWs()
            if (i < s.length && s[i] == ']') { i++; return l }
            while (i < s.length) {
                l.add(parseValue())
                skipWs()
                if (i < s.length && s[i] == ',') { i++; continue }
                if (i < s.length && s[i] == ']') { i++; break }
                break
            }
            return l
        }

        private fun parseString(): String {
            val sb = StringBuilder()
            i++ // opening quote
            while (i < s.length && s[i] != '"') {
                val c = s[i]
                if (c == '\\' && i + 1 < s.length) {
                    i++
                    when (s[i]) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> {
                            val hex = s.substring(i + 1, minOf(i + 5, s.length))
                            sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                            i += 4
                        }
                        else -> sb.append(s[i])
                    }
                } else sb.append(c)
                i++
            }
            i++ // closing quote
            return sb.toString()
        }

        private fun parseBool(): Boolean {
            return if (s.startsWith("true", i)) { i += 4; true } else { i += 5; false }
        }

        private fun parseNumber(): Any {
            val start = i
            while (i < s.length && (s[i].isDigit() || s[i] in "-+.eE")) i++
            val t = s.substring(start, i)
            return t.toIntOrNull() ?: t.toLongOrNull() ?: t.toDoubleOrNull() ?: 0
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }
    }
}

// ---------- 便捷扩展 ----------
@Suppress("UNCHECKED_CAST")
fun Any?.asMap(): Map<String, Any?> = this as? Map<String, Any?> ?: emptyMap()

fun Any?.asList(): List<Any?> = this as? List<Any?> ?: emptyList()

fun Any?.asStr(): String = this?.toString() ?: ""

fun Any?.asIntOr(def: Int): Int = when (this) {
    is Int -> this
    is Long -> toInt()
    is Double -> toInt()
    is String -> toIntOrNull() ?: def
    else -> def
}
