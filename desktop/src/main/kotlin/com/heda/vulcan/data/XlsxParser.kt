package com.heda.vulcan.data

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.zip.ZipFile

/**
 * 轻量级 xlsx 解析器（xlsx 本质是 zip + XML）。
 * 特性：读取每个 sheet 的名称与单元格、识别隐藏行/隐藏列、跳过空值。
 * 纯 Kotlin 实现，零第三方表格库（仅依赖 XML Pull Parser）。
 * 与安卓端 XlsxParser.kt 逻辑一致，行为对齐。
 */
class XlsxParser {

    /** 单个 sheet 的解析结果 */
    data class Sheet(
        val name: String,
        val rows: List<Row>,             // 所有行（含空行，按原顺序）
        val hiddenRowNumbers: Set<Int>,  // 隐藏行号（1-based）
        val hiddenColNumbers: Set<Int>,  // 隐藏列号（1-based）
        val hidden: Boolean = false,     // sheet 本身是否隐藏
        val error: String? = null
    )

    /** 一行数据：colIndex(1-based) -> 文本 */
    data class Row(val rowNumber: Int, val cells: Map<Int, String>)

    data class ParsedBook(
        val sheets: List<Sheet>,
        val errors: List<String> = emptyList()
    )

    fun parse(file: File): ParsedBook {
        val errors = mutableListOf<String>()
        val sheets = mutableListOf<Sheet>()
        try {
            ZipFile(file).use { zip ->
                // 1) sheet 名与 rId 映射
                val wbXml = readEntry(zip, "xl/workbook.xml") ?: run {
                    errors.add("缺少 xl/workbook.xml")
                    return ParsedBook(emptyList(), errors)
                }
                val relsXml = readEntry(zip, "xl/_rels/workbook.xml.rels")
                val shared = readEntry(zip, "xl/sharedStrings.xml")

                val parser = newParser(wbXml)
                val sheetMap = LinkedHashMap<String, Pair<String, Boolean>>() // name -> (rId, hidden)
                parseWorkbookSheets(parser, sheetMap)

                val relMap = HashMap<String, String>()
                if (relsXml != null) parseRels(relsXml, relMap)

                val sharedStrings = if (shared != null) parseSharedStrings(shared) else emptyList()

                for ((name, info) in sheetMap) {
                    val rId = info.first
                    val hidden = info.second
                    val target = relMap[rId] ?: continue
                    val path = resolvePath(target)
                    val sheetXml = readEntry(zip, path)
                    if (sheetXml == null) {
                        errors.add("sheet $name: 找不到文件 $path")
                        continue
                    }
                    try {
                        sheets.add(parseSheet(name, sheetXml, sharedStrings).copy(hidden = hidden))
                    } catch (e: Exception) {
                        errors.add("sheet $name: 解析失败 - ${e.message}")
                        sheets.add(Sheet(name, emptyList(), emptySet(), emptySet(), hidden, e.message))
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("文件打开失败: ${e.message}")
        }
        return ParsedBook(sheets, errors)
    }

    private fun newParser(xml: String): XmlPullParser {
        val p = XmlPullParserFactory.newInstance().newPullParser()
        p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        p.setInput(xml.reader())
        return p
    }

    private fun readEntry(zip: ZipFile, path: String): String? {
        val entry = zip.getEntry(path) ?: return null
        return zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    private fun resolvePath(target: String): String {
        val t = target.trim()
        return when {
            t.startsWith("/") -> t.removePrefix("/")
            t.startsWith("xl/") -> t
            else -> "xl/" + t
        }
    }

    /** 解析 workbook.xml 中的 <sheets><sheet name=.. r:id=.. state=..> */
    private fun parseWorkbookSheets(p: XmlPullParser, out: MutableMap<String, Pair<String, Boolean>>) {
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.name == "sheet") {
                val name = p.getAttributeValue(null, "name") ?: ""
                val state = p.getAttributeValue(null, "state") ?: "visible"
                var rid: String? = null
                for (i in 0 until p.attributeCount) {
                    val n = p.getAttributeName(i)
                    if (n == "id" || n.endsWith(":id")) rid = p.getAttributeValue(i)
                }
                if (name.isNotEmpty() && rid != null) {
                    out[name] = rid to (state == "hidden" || state == "veryHidden")
                }
            }
            event = p.next()
        }
    }

    private fun parseRels(xml: String, out: MutableMap<String, String>) {
        val p = newParser(xml)
        var event = p.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && p.name == "Relationship") {
                val id = p.getAttributeValue(null, "Id") ?: ""
                val target = p.getAttributeValue(null, "Target") ?: ""
                if (id.isNotEmpty() && target.isNotEmpty()) out[id] = target
            }
            event = p.next()
        }
    }

    /** 解析 sharedStrings.xml：返回所有字符串 */
    private fun parseSharedStrings(xml: String): List<String> {
        val result = mutableListOf<String>()
        val p = newParser(xml)
        var event = p.eventType
        var inSi = false
        var inT = false
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inSi = true; sb.setLength(0) }
                    "t" -> if (inSi) inT = true
                }
                XmlPullParser.TEXT -> if (inT) sb.append(p.text ?: "")
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "si" -> { result.add(sb.toString().trim()); inSi = false }
                }
            }
            event = p.next()
        }
        return result
    }

    /** 解析一个 worksheet */
    private fun parseSheet(name: String, xml: String, shared: List<String>): Sheet {
        val rows = mutableListOf<Row>()
        val hiddenRows = mutableSetOf<Int>()
        val hiddenCols = mutableSetOf<Int>()
        val p = newParser(xml)
        var event = p.eventType
        var curRowNum = 0
        var curCells = LinkedHashMap<Int, String>()
        var rowHidden = false
        var cellRef = ""
        var cellType = ""
        var inV = false
        var inIs = false
        var inT = false
        val cellSb = StringBuilder()
        var inCols = false

        fun flushRow() {
            if (curRowNum > 0) {
                rows.add(Row(curRowNum, curCells))
                if (rowHidden) hiddenRows.add(curRowNum)
                curCells = LinkedHashMap()
            }
        }

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "cols" -> inCols = true
                    "col" -> if (inCols) {
                        val min = p.getAttributeValue(null, "min")?.toIntOrNull()
                        val max = p.getAttributeValue(null, "max")?.toIntOrNull()
                        val hid = p.getAttributeValue(null, "hidden") == "1"
                        if (hid && min != null && max != null) for (c in min..max) hiddenCols.add(c)
                    }
                    "row" -> {
                        curRowNum = p.getAttributeValue(null, "r")?.toIntOrNull() ?: 0
                        rowHidden = p.getAttributeValue(null, "hidden") == "1"
                        cellSb.setLength(0)
                    }
                    "c" -> {
                        cellRef = p.getAttributeValue(null, "r") ?: ""
                        cellType = p.getAttributeValue(null, "t") ?: ""
                        cellSb.setLength(0)
                        inV = false; inIs = false; inT = false
                    }
                    "v" -> inV = true
                    "is" -> inIs = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> {
                    if (inV) cellSb.append(p.text ?: "")
                    else if (inT && inIs) cellSb.append(p.text ?: "")
                }
                XmlPullParser.END_TAG -> when (p.name) {
                    "cols" -> inCols = false
                    "v", "t", "is" -> { inV = false; inT = false; inIs = false }
                    "c" -> {
                        val col = colIndexFromRef(cellRef)
                        if (col > 0 && col !in hiddenCols) {
                            val raw = cellSb.toString().trim()
                            val text = when (cellType) {
                                "s" -> raw.toIntOrNull()?.let { shared.getOrNull(it) ?: "" } ?: ""
                                "inlineStr" -> raw
                                else -> raw // 数字/str/bool/date 等直接取文本
                            }
                            if (text.isNotEmpty()) curCells[col] = text
                        }
                    }
                    "row" -> flushRow()
                }
            }
            event = p.next()
        }
        return Sheet(name, rows, hiddenRows, hiddenCols)
    }

    companion object {
        /** "A" -> 1, "AA" -> 27 */
        fun colIndexFromRef(ref: String): Int {
            var i = 0
            var idx = 0
            while (i < ref.length && ref[i].isLetter()) {
                idx = idx * 26 + (ref[i].uppercaseChar() - 'A' + 1)
                i++
            }
            return idx
        }
    }
}
