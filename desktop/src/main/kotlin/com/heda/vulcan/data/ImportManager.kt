package com.heda.vulcan.data

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 导入管理器：把解析后的 xlsx sheet 转换为数据库记录。
 * 自动识别表类型、定位表头行、映射列，跳过空行/隐藏行/隐藏列。
 * 与安卓端 ImportManager.kt 逻辑一致（纯 JVM，无平台依赖）。
 */
object ImportManager {

    // ---------------- 表类型识别 ----------------
    fun detectType(book: XlsxParser.ParsedBook): TableType {
        for (s in book.sheets) if (findJudgeHeader(s) != null) return TableType.JUDGE
        for (s in book.sheets) if (findProcessHeader(s) != null) return TableType.PROCESS
        for (s in book.sheets) if (findHeightHeader(s) != null) return TableType.HEIGHT
        for (s in book.sheets) if (findDefectHeader(s) != null) return TableType.DEFECT
        return TableType.UNKNOWN
    }

    private fun findJudgeHeader(sheet: XlsxParser.Sheet, maxScan: Int = 10): Pair<Int, Map<String, Int>>? {
        val scanEnd = minOf(maxScan, sheet.rows.size)
        for (i in 0 until scanEnd) {
            var codeCol = -1; var causeCol = -1; var deptCol = -1; var countCol = -1; var nameCol = -1
            for ((col, text) in sheet.rows[i].cells) {
                val t = text.trim()
                if (t.contains("不良代码")) { if (codeCol < 0) codeCol = col }
                if (t.contains("原因分析")) { if (causeCol < 0) causeCol = col }
                if (t.contains("责任科室") || t.contains("科室")) { if (deptCol < 0) deptCol = col }
                if (t.contains("中文名称") || t.contains("名称")) { if (nameCol < 0) nameCol = col }
                if (t.contains("条数")) { if (countCol < 0) countCol = col }
            }
            if (codeCol > 0 && causeCol > 0 && deptCol > 0 && countCol > 0) {
                return i to mapOf(
                    "code" to codeCol, "cause" to causeCol, "dept" to deptCol,
                    "count" to countCol, "name" to nameCol
                )
            }
        }
        return null
    }

    private fun cellOf(sheet: XlsxParser.Sheet, rowIdx: Int, col: Int): String =
        sheet.rows.getOrNull(rowIdx)?.cells?.get(col)?.trim() ?: ""

    private fun findProcessHeader(sheet: XlsxParser.Sheet, maxScan: Int = 10): Pair<Int, Map<String, Int>>? {
        val scanEnd = minOf(maxScan, sheet.rows.size)
        for (i in 0 until scanEnd) {
            val cells = sheet.rows[i].cells
            var hasMaterial = false
            var hasTimeCode = false
            val mapping = HashMap<String, Int>()
            for ((col, text) in cells) {
                val t = text.trim()
                when {
                    t.contains("胎胚物料") || t.contains("物料") -> { mapping["material"] = col; hasMaterial = true }
                    t.contains("硫化时间代码") || t.contains("时间代码") || (t.contains("硫化时间") && !t.contains("代码")) -> {
                        mapping["timeCode"] = col; hasTimeCode = true
                    }
                    t.contains("硫化程序") || t == "程序" -> mapping["program"] = col
                    t.contains("胶囊规格") || t.contains("胶囊") -> mapping["capsule"] = col
                    t.contains("英寸") -> mapping["inch"] = col
                    t.contains("外温") -> mapping["externalTemp"] = col
                    t.contains("合模力") -> mapping["clampForce"] = col
                    t.contains("PCI压力") || t.contains("PCI 压力") || t.contains("pci压力") -> mapping["pciPressure"] = col
                    t.contains("PCI高度") || t.contains("PCI 高度") -> mapping["pciHeight"] = col
                    t.contains("胶种") -> mapping["rubber"] = col
                    t.contains("钢圈代码") || t.contains("钢圈") -> mapping["beadCode"] = col
                    t.contains("钢圈角度") -> mapping["beadAngle"] = col
                    t.contains("生效") || t.contains("更新") || t == "日期" -> mapping["effectiveDate"] = col
                }
            }
            if (hasMaterial && hasTimeCode) return i to mapping
        }
        return null
    }

    private fun findHeightHeader(sheet: XlsxParser.Sheet, maxScan: Int = 8): Pair<Int, String>? {
        val scanEnd = minOf(maxScan, sheet.rows.size)
        for (i in 0 until scanEnd) {
            var hasCapsule = false; var hasHeight = false; var hasModel = false; var hasStretch = false
            for ((_, text) in sheet.rows[i].cells) {
                val t = text.trim()
                if (t.contains("胶囊规格") || t.contains("胶囊")) hasCapsule = true
                if (t.contains("拉直高度") || t.contains("拉伸高度") || t.contains("高度")) hasHeight = true
                if (t == "型号") hasModel = true
                if (t.contains("拉伸高度")) hasStretch = true
            }
            if (hasCapsule && hasHeight) return i to "flat"
            if (hasModel && hasStretch) return i to "grouped"
        }
        return null
    }

    private fun findDefectHeader(sheet: XlsxParser.Sheet, maxScan: Int = 6): Pair<Int, Map<String, Int>>? {
        val scanEnd = minOf(maxScan, sheet.rows.size)
        var titleCandidate: Pair<Int, Map<String, Int>>? = null
        for (i in 0 until scanEnd) {
            val cells = sheet.rows[i].cells
            val texts = cells.values.map { it.trim() }.filter { it.isNotEmpty() }
            if (texts.isNotEmpty() && texts.all { it.contains("简称") } && texts.size > 1) continue
            var codeCol = -1; var nameCol = -1; var descCol = -1; var hit = 0
            for ((col, text) in cells) {
                val t = text.trim()
                if (t.contains("简称")) { if (codeCol < 0) codeCol = col; hit++ }
                if (t.contains("名称")) { if (nameCol < 0) nameCol = col; hit++ }
                if (t.contains("说明") || t.contains("描述")) { if (descCol < 0) descCol = col; hit++ }
            }
            if (hit >= 2) {
                val m = HashMap<String, Int>()
                if (codeCol > 0) m["code"] = codeCol
                if (nameCol > 0) m["name"] = nameCol
                if (descCol > 0) m["desc"] = descCol
                return i to m
            }
            if (hit == 1 && titleCandidate == null) titleCandidate = i to emptyMap()
        }
        return titleCandidate
    }

    // ---------------- 导入工艺标准 ----------------
    fun importProcess(sheets: List<XlsxParser.Sheet>): Pair<List<ProcessSpec>, ImportStats> {
        val specs = mutableListOf<ProcessSpec>()
        var skippedEmpty = 0; var hiddenRows = 0; var hiddenCols = 0
        val usedSheets = mutableListOf<String>()
        var errorCount = 0

        for (sheet in sheets) {
            if (sheet.hidden || sheet.error != null) { if (sheet.error != null) errorCount++; continue }
            val pattern = sheet.name.trim()
            if (pattern.isEmpty() || pattern.startsWith("~") || pattern.startsWith(".") || pattern.startsWith("#")) continue
            val (hdrIdx, mapping) = findProcessHeader(sheet) ?: continue
            val materialCol = mapping["material"] ?: continue
            hiddenRows += sheet.hiddenRowNumbers.size
            hiddenCols += sheet.hiddenColNumbers.size

            var effCol = mapping["effectiveDate"]
            if (effCol == null) {
                val maxMapped = mapping.values.maxOrNull() ?: 0
                val candidate = sheet.rows.drop(hdrIdx + 1)
                    .firstOrNull { row -> row.cells.keys.any { it > maxMapped } }
                    ?.cells?.keys?.maxOrNull()
                if (candidate != null && candidate > maxMapped) effCol = candidate
            }

            for (ri in (hdrIdx + 1) until sheet.rows.size) {
                val row = sheet.rows[ri]
                if (row.rowNumber in sheet.hiddenRowNumbers) continue
                val material = (row.cells[materialCol] ?: "").trim()
                if (material.isEmpty()) { skippedEmpty++; continue }
                if (material.contains("胎胚物料") || material.contains("物料名称") || material.contains("备注")) continue

                specs.add(
                    ProcessSpec(
                        pattern = pattern,
                        material = material,
                        size = MaterialParser.extractSize(material),
                        region = MaterialParser.extractRegion(material) ?: "",
                        program = str(row, mapping["program"]),
                        timeCode = str(row, mapping["timeCode"]),
                        capsule = str(row, mapping["capsule"]),
                        inch = str(row, mapping["inch"]),
                        externalTemp = str(row, mapping["externalTemp"]),
                        clampForce = str(row, mapping["clampForce"]),
                        pciPressure = str(row, mapping["pciPressure"]),
                        pciHeight = str(row, mapping["pciHeight"]),
                        rubber = str(row, mapping["rubber"]),
                        beadCode = str(row, mapping["beadCode"]),
                        beadAngle = str(row, mapping["beadAngle"]),
                        effectiveDate = effCol?.let { str(row, it) } ?: ""
                    )
                )
            }
            usedSheets.add(pattern)
        }

        return specs to ImportStats(
            type = TableType.PROCESS.label, sheetsFound = usedSheets.size, rowsImported = specs.size,
            rowsSkippedEmpty = skippedEmpty, hiddenRowsSkipped = hiddenRows, hiddenColsSkipped = hiddenCols,
            sheetNames = usedSheets,
            message = if (errorCount > 0) "$errorCount 个sheet解析失败" else ""
        )
    }

    private fun str(row: XlsxParser.Row, col: Int?): String =
        if (col != null) (row.cells[col] ?: "").trim() else ""

    // ---------------- 导入拉直高度 ----------------
    fun importHeight(sheets: List<XlsxParser.Sheet>): Pair<List<CapsuleHeight>, ImportStats> {
        val items = mutableListOf<CapsuleHeight>()
        var skippedEmpty = 0; var hiddenRows = 0; var hiddenCols = 0
        val usedSheets = mutableListOf<String>()

        for (sheet in sheets) {
            if (sheet.hidden || sheet.error != null) continue
            val (hdrIdx, mode) = findHeightHeader(sheet) ?: continue
            hiddenRows += sheet.hiddenRowNumbers.size
            hiddenCols += sheet.hiddenColNumbers.size
            usedSheets.add(sheet.name.trim())

            if (mode == "flat") {
                val hdrRow = sheet.rows[hdrIdx].cells
                var capCol = 0; var manCol = 0; var hCol = 0; var perCol = 0; var diffCol = 0
                for ((col, text) in hdrRow) {
                    val t = text.trim()
                    if (t.contains("胶囊规格") || t.contains("胶囊")) capCol = col
                    if (t.contains("厂家")) manCol = col
                    if (t.contains("拉直高度") || t.contains("拉伸高度")) hCol = col
                    if (t.contains("断面周长")) perCol = col
                    if (t == "差") diffCol = col
                }
                for (ri in (hdrIdx + 1) until sheet.rows.size) {
                    val row = sheet.rows[ri]
                    if (row.rowNumber in sheet.hiddenRowNumbers) continue
                    val cap = str(row, capCol)
                    if (cap.isEmpty()) { skippedEmpty++; continue }
                    items.add(CapsuleHeight(cap, str(row, manCol), str(row, hCol), str(row, perCol), str(row, diffCol)))
                }
            } else {
                val hdrRow = sheet.rows[hdrIdx].cells
                val groups = mutableListOf<IntArray>()
                for (c in hdrRow.keys.sorted()) {
                    val t = hdrRow[c]?.trim() ?: ""
                    if (t == "型号" || t.contains("型号")) groups.add(intArrayOf(c, c + 1, c + 2))
                }
                for (ri in (hdrIdx + 1) until sheet.rows.size) {
                    val row = sheet.rows[ri]
                    if (row.rowNumber in sheet.hiddenRowNumbers) continue
                    for (g in groups) {
                        val cap = str(row, g[0])
                        if (cap.isEmpty()) continue
                        items.add(CapsuleHeight(cap, str(row, g[1]), str(row, g[2]), "", ""))
                    }
                }
            }
        }

        return items to ImportStats(
            type = TableType.HEIGHT.label, sheetsFound = usedSheets.size, rowsImported = items.size,
            rowsSkippedEmpty = skippedEmpty, hiddenRowsSkipped = hiddenRows,
            hiddenColsSkipped = hiddenCols, sheetNames = usedSheets
        )
    }

    // ---------------- 导入不良简称 ----------------
    fun importDefect(sheets: List<XlsxParser.Sheet>): Pair<List<Defect>, ImportStats> {
        val items = mutableListOf<Defect>()
        var skippedEmpty = 0; var hiddenRows = 0; var hiddenCols = 0
        val usedSheets = mutableListOf<String>()
        val seen = HashSet<String>()

        for (sheet in sheets) {
            if (sheet.hidden || sheet.error != null) continue
            val hdr = findDefectHeader(sheet)
            var codeCol = 2; var nameCol = 3; var descCol = 4
            var startIdx = 1
            if (hdr != null) {
                val (hdrIdx, mapping) = hdr
                startIdx = hdrIdx + 1
                if (mapping.isNotEmpty()) {
                    codeCol = mapping["code"] ?: 2
                    nameCol = mapping["name"] ?: 3
                    descCol = mapping["desc"] ?: 4
                }
            } else {
                val first = cellOf(sheet, 0, 2)
                startIdx = if (first.isNotEmpty() && !isCode(first)) 1 else 0
            }
            hiddenRows += sheet.hiddenRowNumbers.size
            hiddenCols += sheet.hiddenColNumbers.size
            usedSheets.add(sheet.name.trim())

            for (ri in startIdx until sheet.rows.size) {
                val row = sheet.rows[ri]
                if (row.rowNumber in sheet.hiddenRowNumbers) continue
                val code = str(row, codeCol)
                if (code.isEmpty()) { skippedEmpty++; continue }
                if (!isCode(code)) continue
                val name = str(row, nameCol)
                if (name.isEmpty()) continue
                val desc = str(row, descCol)
                if (seen.add(code.uppercase())) {
                    items.add(Defect(code = code, name = name, description = desc))
                }
            }

            val fmNotes = collectFmNotes(sheet)
            if (fmNotes.isNotEmpty()) {
                val fmItems = parseFmItems(fmNotes.joinToString(" "))
                if (fmItems.isNotEmpty()) {
                    val idx = items.indexOfFirst { it.code.equals("FM", ignoreCase = true) }
                    if (idx >= 0) {
                        val old = items[idx]
                        items[idx] = old.copy(
                            description = old.description + "\n\nFM分类明细（" + fmItems.size + "种）：\n" +
                                    fmItems.joinToString("\n")
                        )
                    }
                }
            }
        }

        return items to ImportStats(
            type = TableType.DEFECT.label, sheetsFound = usedSheets.size, rowsImported = items.size,
            rowsSkippedEmpty = skippedEmpty, hiddenRowsSkipped = hiddenRows,
            hiddenColsSkipped = hiddenCols, sheetNames = usedSheets
        )
    }

    private fun isCode(s: String): Boolean {
        if (s.isEmpty() || s.length > 8) return false
        if (s.any { it.isWhitespace() }) return false
        return s.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    private val fmNumRegex = Regex("""\d{1,2}\s*[)）]""")

    private fun parseFmItems(text: String): List<String> {
        val items = mutableListOf<String>()
        val starts = fmNumRegex.findAll(text).map { it.range.first }.toList()
        if (starts.isEmpty()) return emptyList()
        for (i in starts.indices) {
            val start = starts[i]
            val end = if (i + 1 < starts.size) starts[i + 1] else text.length
            val item = text.substring(start, end).replace(Regex("\\s+"), " ").trim()
            if (item.length >= 4) items.add(item)
        }
        return items
    }

    private fun collectFmNotes(sheet: XlsxParser.Sheet): List<String> {
        val notes = LinkedHashSet<String>()
        var collecting = false
        for (row in sheet.rows) {
            val hasTitle = row.cells.values.any { it.contains("FM分类") }
            if (hasTitle) {
                collecting = true
                for (text in row.cells.values) {
                    val t = text.trim()
                    if (!t.contains("FM分类") && t.length > 10) notes.add(t)
                }
                continue
            }
            if (collecting) {
                val c1 = (row.cells[1] ?: "").trim()
                val c2 = (row.cells[2] ?: "").trim()
                val longTexts = row.cells.values.filter { it.trim().length > 10 }
                if (c1.isEmpty() && c2.isEmpty() && longTexts.isNotEmpty()) {
                    longTexts.forEach { notes.add(it.trim()) }
                    continue
                }
                collecting = false
            }
        }
        return notes.toList()
    }

    // ---------------- 导入判胎分析 ----------------
    fun importDefectCauses(sheets: List<XlsxParser.Sheet>): Pair<List<DefectCause>, ImportStats> {
        val items = mutableListOf<DefectCause>()
        var skippedEmpty = 0
        val usedSheets = mutableListOf<String>()
        val seen = HashSet<String>()

        for (sheet in sheets) {
            if (sheet.hidden || sheet.error != null) continue
            val (hdrIdx, mapping) = findJudgeHeader(sheet) ?: continue
            val codeCol = mapping["code"] ?: continue
            val causeCol = mapping["cause"]!!
            val deptCol = mapping["dept"]!!
            val countCol = mapping["count"]
            val nameCol = mapping["name"]
            usedSheets.add(sheet.name.trim())

            for (ri in (hdrIdx + 1) until sheet.rows.size) {
                val row = sheet.rows[ri]
                if (row.rowNumber in sheet.hiddenRowNumbers) continue
                val code = str(row, codeCol).uppercase().trim()
                if (code.isEmpty()) { skippedEmpty++; continue }
                if (code.all { it.isDigit() } || code.contains("合计") || code.contains("总计")) continue
                val cause = str(row, causeCol)
                if (cause.isEmpty()) { skippedEmpty++; continue }
                if (cause.all { it.isDigit() }) continue
                if (!seen.add(code + "|" + cause + "|" + str(row, deptCol))) continue
                items.add(
                    DefectCause(code, str(row, nameCol), cause, str(row, deptCol), str(row, countCol).toIntOrNull() ?: 0)
                )
            }
        }

        return items to ImportStats(
            type = TableType.JUDGE.label, sheetsFound = usedSheets.size, rowsImported = items.size,
            rowsSkippedEmpty = skippedEmpty, sheetNames = usedSheets
        )
    }

    fun nowText(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

    // ---------------- 编排：识别类型 → 解析 → 写库 ----------------
    /** 解析文件并识别类型，返回 (类型, ParsedBook) */
    fun peek(file: File): Pair<TableType, XlsxParser.ParsedBook> {
        val book = XlsxParser().parse(file)
        return detectType(book) to book
    }

    /** 执行导入（按识别出的类型写库），返回统计 */
    fun applyImport(db: DesktopDb, file: File, forced: TableType? = null): ImportStats {
        val (detected, book) = peek(file)
        val type = forced ?: detected

        val stats = when (type) {
            TableType.PROCESS -> {
                val (items, st) = importProcess(book.sheets); db.clearProcess(); db.insertProcessBatch(items); st
            }
            TableType.HEIGHT -> {
                val (items, st) = importHeight(book.sheets); db.clearHeight(); db.insertHeightBatch(items); st
            }
            TableType.DEFECT -> {
                val (items, st) = importDefect(book.sheets); db.clearDefect(); db.insertDefectBatch(items); st
            }
            TableType.JUDGE -> {
                val (items, st) = importDefectCauses(book.sheets); db.clearJudge(); db.insertJudgeBatch(items); st
            }
            else -> ImportStats(type = TableType.UNKNOWN.label, message = "无法识别表类型")
        }
        if (book.errors.isNotEmpty() && stats.message.isEmpty()) {
            // errors 已并入 message 逻辑，这里仅记录
        }
        db.setMeta("last_import", nowText())
        return stats
    }
}
