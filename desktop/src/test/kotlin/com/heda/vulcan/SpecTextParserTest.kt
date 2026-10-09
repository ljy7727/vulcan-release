package com.heda.vulcan

import com.heda.vulcan.data.SpecTextParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** SpecTextParser 剪贴板文字识别测试（样例来自真实物料串与原版首缸数据）。 */
class SpecTextParserTest {

    @Test
    fun 解析标准物料串() {
        val p = SpecTextParser.parse("225/50ZR17 XL RU06 4 98W CH LB DURUN")
        assertEquals("225/50ZR17", p.size)
        assertEquals("CH", p.region)
        assertEquals("RU06", p.pattern)
        assertTrue(p.usable)
    }

    @Test
    fun 解析首缸样例_13寸C规格() {
        val p = SpecTextParser.parse("165R13C C212 8 94/93R CN LB DURUN")
        assertEquals("165R13C", p.size)
        assertEquals("CN", p.region)
        assertEquals("C212", p.pattern)
        assertTrue(p.usable)
    }

    @Test
    fun 识别机台号() {
        val p = SpecTextParser.parse("1104 175/50R15 BC7778 4 75T CH LB DURUN")
        assertEquals("175/50R15", p.size)
        assertEquals("BC7778", p.pattern)
        assertEquals("1104", p.machine)
        assertEquals("CH", p.region)
    }

    @Test
    fun 忽略品牌词与载重指数() {
        // KAPSEN / LB / XL 是品牌词，98W 是载重指数，都不应被当花纹
        val p = SpecTextParser.parse("205/55R16 91V KAPSEN KH21 CH")
        assertEquals("KH21", p.pattern)
        assertEquals("205/55R16", p.size)
        assertEquals("CH", p.region)
    }

    @Test
    fun 已知花纹库优先精确命中() {
        val p = SpecTextParser.parse("215/60R17 RU06 96H CN", listOf("RU06", "S701"))
        assertEquals("RU06", p.pattern)
    }

    @Test
    fun 无规格时不可用() {
        val p = SpecTextParser.parse("随便一段文字没有规格")
        assertFalse(p.usable)
    }

    @Test
    fun describe输出格式() {
        val p = SpecTextParser.parse("225/50ZR17 XL RU06 4 98W CH LB DURUN")
        val d = SpecTextParser.describe(p)
        assertTrue(d.contains("花纹 RU06"))
        assertTrue(d.contains("规格 225/50ZR17"))
        assertTrue(d.contains("区域 CH"))
    }

    @Test
    fun 区域缺失时describe显示全部() {
        val p = SpecTextParser.parse("215/60R17 RU06 96H")
        val d = SpecTextParser.describe(p)
        assertTrue(d.contains("区域 全部"))
    }
}
