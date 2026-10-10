package com.heda.vulcan.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.heda.vulcan.data.CapsuleAlias
import com.heda.vulcan.data.Defect
import com.heda.vulcan.data.DefectCause
import com.heda.vulcan.data.FirstCure
import com.heda.vulcan.data.ImportManager
import com.heda.vulcan.data.JudgeGuide
import com.heda.vulcan.data.Json
import com.heda.vulcan.data.MaterialParser
import com.heda.vulcan.data.MachineList
import com.heda.vulcan.data.ProcessSpec
import com.heda.vulcan.data.AppVersion
import com.heda.vulcan.data.SpecTextParser
import com.heda.vulcan.data.TableType
import com.heda.vulcan.data.Updater
import com.heda.vulcan.data.TimeCode
import com.heda.vulcan.data.asList
import com.heda.vulcan.data.asMap
import com.heda.vulcan.data.asStr
import com.heda.vulcan.core.AppLog
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------- 通用小组件 ----------------

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Card2(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) { content() }
    }
}

@Composable
private fun KvRow(k: String, v: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(k, fontSize = 13.sp, color = MaterialTheme.colorScheme.outline, modifier = Modifier.width(96.dp))
        Text(v, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------- 1. 查询 ----------------

@Composable
fun QueryScreen(state: AppState) {
    SectionTitle("工艺查询")

    Card2 {
        var patternMenu by remember { mutableStateOf(false) }
        var regionMenu by remember { mutableStateOf(false) }
        var shortcut by remember { mutableStateOf("") }

        // 花纹
        Column {
            Text("花纹（规格）", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Box {
                OutlinedButton(onClick = { patternMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.queryPattern.ifEmpty { "选择花纹（共 ${state.patterns.size}）" })
                }
                DropdownMenu(expanded = patternMenu, onDismissRequest = { patternMenu = false }) {
                    state.patterns.take(200).forEach { p ->
                        DropdownMenuItem(text = { Text(p) }, onClick = { state.queryPattern = p; patternMenu = false })
                    }
                }
            }
        }

        Box(Modifier.height(6.dp))
        Field("规格尺寸", state.querySize, { state.querySize = it })

        // 区域
        val regions = if (state.queryPattern.isNotEmpty() && state.querySize.isNotEmpty())
            state.db.queryRegions(state.queryPattern, state.querySize) else emptyList()
        Box(Modifier.height(6.dp))
        Column {
            Text("销售区域（不选=全部）", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Box {
                OutlinedButton(onClick = { regionMenu = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(state.queryRegion.ifEmpty { if (regions.isEmpty()) "全部区域" else "选择区域" })
                }
                DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                    DropdownMenuItem(text = { Text("全部区域") }, onClick = { state.queryRegion = ""; regionMenu = false })
                    regions.forEach { r ->
                        DropdownMenuItem(text = { Text(r) }, onClick = { state.queryRegion = r; regionMenu = false })
                    }
                }
            }
        }

        Box(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { doQuery(state) }) { Text("查询") }
            Button(onClick = { readClipboardToQuery(state) }) { Text("读剪贴板") }
            OutlinedButton(onClick = {
                state.queryPattern = ""; state.querySize = ""; state.queryRegion = ""
                state.queryResult = null; state.queryCandidates = emptyList()
            }) { Text("一键清空") }
        }

        Box(Modifier.height(10.dp))
        HorizontalDivider()
        Box(Modifier.height(8.dp))
        Field("快捷识别（粘贴整条物料）", shortcut, { shortcut = it })
        Box(Modifier.height(6.dp))
        OutlinedButton(onClick = {
            val (sz, pat, reg) = MaterialParser.extractFromFull(shortcut, state.patterns)
            if (pat != null) state.queryPattern = pat
            if (sz != null) state.querySize = sz
            if (reg != null) state.queryRegion = reg
            state.setStatus("快捷识别：花纹=$pat 尺寸=$sz 区域=$reg")
        }) { Text("识别") }
    }

    // 候选列表
    if (state.queryCandidates.size > 1) {
        Card2 {
            Text("匹配到多条，请选择：", fontSize = 13.sp)
            state.queryCandidates.forEach { s ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { pickSpec(state, s) }.padding(vertical = 8.dp)
                ) {
                    Text("[${s.region}] ${s.material}", fontSize = 13.sp)
                }
            }
        }
    }

    // 结果卡片
    state.queryResult?.let { s -> SpecCard(state, s) }
}

/**
 * 原版「读剪贴板」逻辑：先读剪贴板文本走 SpecTextParser；文本不可用时
 * 原版会继续读剪贴板图片走 OCR（重建版 OCR 未接入，先提示）。
 */
private fun readClipboardToQuery(state: AppState) {
    val txt = DesktopUtil.clipboardText()
    if (txt != null) {
        val p = SpecTextParser.parse(txt, state.patterns)
        if (p.usable) {
            var pattern = p.pattern!!
            var aliasNote = ""
            // 花纹不在已知库 → 尝试「胶囊对齐」换算（忽略大小写）
            val mapped = CapsuleAlias.resolvePattern(pattern, state.patterns, state.aliasLookup)
            if (mapped != null) {
                aliasNote = " · 「$pattern」按胶囊对齐对应「$mapped」"
                AppLog.line("[识别] 胶囊对齐换算：$pattern → $mapped")
                pattern = mapped
            }
            state.queryPattern = pattern
            state.querySize = p.size
            val regions = state.db.queryRegions(pattern, p.size)
            state.queryRegion = p.region ?: if (regions.size == 1) regions[0] else ""
            state.setStatus("已从剪贴板文字识别：" + SpecTextParser.describe(p) + aliasNote)
            doQuery(state)
        } else {
            state.setStatus("剪贴板文字里没找到规格（内容：" + txt.replace("\n", " ").take(40) + "）")
        }
    } else {
        state.setStatus("剪贴板里既没有文字也没有图片：请复制那行物料文字，或先截图再点这里")
    }
}

private fun doQuery(state: AppState) {
    if (state.queryPattern.isEmpty() || state.querySize.isEmpty()) {
        state.setStatus("请先选择花纹并填写规格尺寸")
        return
    }
    var list = state.db.querySpecs(state.queryPattern, state.querySize)
    if (state.queryRegion.isNotEmpty()) list = list.filter { it.region == state.queryRegion }
    when {
        list.isEmpty() -> {
            state.queryResult = null; state.queryCandidates = emptyList()
            state.setStatus("未找到该规格的工艺（请确认花纹/尺寸）")
        }
        list.size == 1 -> pickSpec(state, list[0])
        else -> {
            state.queryCandidates = list
            state.queryResult = null
            state.setStatus("匹配到 ${list.size} 条，请在上方选择")
        }
    }
}

private fun pickSpec(state: AppState, s: ProcessSpec) {
    state.queryResult = s
    state.queryCandidates = emptyList()
    state.setStatus("查询成功：${s.pattern} ${s.size} ${s.region}")
}

@Composable
private fun SpecCard(state: AppState, s: ProcessSpec) {
    val tc = TimeCode.parse(s.timeCode, state.extraSeconds)
    val height = state.db.queryHeight(s.capsule)?.height ?: ""
    Card2 {
        Text("工艺卡片", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 6.dp))
        KvRow("花纹", s.pattern)
        KvRow("规格", s.size)
        KvRow("区域", s.region)
        KvRow("模套温度", tc?.temp?.let { "${it} ℃" } ?: "—")
        KvRow("硫化时间", tc?.finalText ?: "—")
        KvRow("解码过程", tc?.let { "${it.baseText}（${TimeCode.letterLabel(it.letter)}）+ 机动 ${it.extraSeconds}秒" } ?: "—")
        KvRow("胶囊规格", CapsuleAlias.display(s.capsule, state.aliasLookup))
        KvRow("拉直高度", height.ifEmpty { "—" })
        KvRow("合模力", s.clampForce)
        KvRow("PCI压力", s.pciPressure)
        KvRow("PCI高度", s.pciHeight)
        KvRow("硫化程序", s.program)
        KvRow("外温", s.externalTemp)
        KvRow("胶种", s.rubber)
        KvRow("钢圈", "${s.beadCode} ${s.beadAngle}")
        KvRow("生效日期", s.effectiveDate)
        Box(Modifier.height(10.dp))
        Button(onClick = {
            val now = System.currentTimeMillis()
            state.db.insertFirstCure(
                FirstCure(
                    id = 0, machine = "", pattern = s.pattern, material = s.material, size = s.size,
                    region = s.region, timeCode = s.timeCode, temp = tc?.temp?.toString() ?: "",
                    finalTime = tc?.finalText ?: "", capsule = s.capsule, height = height,
                    clampForce = s.clampForce, pciPressure = s.pciPressure, pciHeight = s.pciHeight,
                    extra = "", createdAt = now
                )
            )
            state.firstCures.clear(); state.firstCures.addAll(state.db.queryFirstCures())
            state.setStatus("已存入首缸记录")
        }) { Text("展示为首缸") }
    }
}

// ---------------- 2. 首缸 ----------------

/**
 * 首缸记录：搜索 + 批量勾选（全选/反选/清空）+ 导出长图 + 打开导出目录 + 批量删除。
 * 列表版式与网页版一致（机台标签 / 尺寸·花纹 / 四列参数），导出图与三端同版式。
 */
@Composable
fun FirstCureScreen(state: AppState) {
    var search by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    val checked = remember { mutableStateMapOf<Long, Boolean>() }

    val filtered = state.firstCures.filter { f ->
        search.isBlank() ||
                f.material.contains(search, true) || f.machine.contains(search, true) ||
                f.size.contains(search, true) || f.capsule.contains(search, true)
    }
    val checkedCount = filtered.count { checked[it.id] == true }

    SectionTitle("首缸记录")
    Card2 {
        Field("搜索（物料/机台/尺寸/胶囊）", search, { search = it })
        Box(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { filtered.forEach { checked[it.id] = true } }) { Text("全选") }
            OutlinedButton(onClick = { filtered.forEach { checked[it.id] = !(checked[it.id] ?: false) } }) { Text("反选") }
            OutlinedButton(onClick = { checked.clear() }) { Text("清空选择") }
            Box(Modifier.weight(1f))
            Text("已选 $checkedCount/${filtered.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { exportSelected(state, filtered.filter { checked[it.id] == true }) },
                enabled = checkedCount > 0
            ) { Text("导出图片（$checkedCount 条）") }
            OutlinedButton(onClick = { openExportDir(state) }) { Text("打开导出目录") }
            OutlinedButton(
                onClick = { confirmDeleteSelected = true },
                enabled = checkedCount > 0
            ) { Text("删除选中（$checkedCount 条）") }
            Box(Modifier.weight(1f))
            OutlinedButton(onClick = { confirmClear = true }, enabled = state.firstCures.isNotEmpty()) {
                Text("清空全部", color = MaterialTheme.colorScheme.error)
            }
        }
        Box(Modifier.height(6.dp))
        Text(
            "导出目录：${exportDir(state).absolutePath}（可在「设置」中更改）",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
        )
    }

    if (state.firstCures.isEmpty()) {
        Text("暂无首缸记录（在查询页点「展示为首缸」后保存到这里）", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
        return
    }

    filtered.forEach { f ->
        var machineMenu by remember { mutableStateOf(false) }
        val isChecked = checked[f.id] == true
        Card2 {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isChecked, onCheckedChange = { checked[f.id] = it })
                if (f.machine.isNotEmpty()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(f.machine, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box(Modifier.weight(1f))
                Text(
                    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(f.createdAt)),
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
                )
            }
            // 尺寸 · 花纹（点击展开详情）
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    expandedId = if (expandedId == f.id) null else f.id
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(f.size, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Box(Modifier.width(8.dp))
                Text(f.pattern, fontSize = 14.sp)
                Box(Modifier.weight(1f))
                if (f.region.isNotEmpty()) {
                    Text(f.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold)
                }
            }
            Text(f.material, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Box(Modifier.height(6.dp))
            // 四列参数
            Row(Modifier.fillMaxWidth()) {
                MiniParam("硫化时间", f.finalTime, Modifier.weight(1f))
                MiniParam("模套温度", f.temp.ifEmpty { "—" }, Modifier.weight(1f), "℃")
                MiniParam("胶囊", CapsuleAlias.display(f.capsule, state.aliasLookup), Modifier.weight(1f))
                MiniParam("拉直高度", f.height.ifEmpty { "—" }, Modifier.weight(1f), "mm")
            }
            if (expandedId == f.id) {
                Box(Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                KvRow("合模力", "${f.clampForce.ifEmpty { "—" }} KN")
                KvRow("PCI压力", "${f.pciPressure.ifEmpty { "—" }} MPa")
                KvRow("PCI高度", "${f.pciHeight.ifEmpty { "—" }} mm")
                KvRow("时间代码", f.timeCode.ifEmpty { "—" })
                if (f.extra.isNotEmpty()) KvRow("其他", f.extra)
                Box(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box {
                        OutlinedButton(onClick = { machineMenu = true }) {
                            Text(if (f.machine.isEmpty()) "选机台" else "改机台")
                        }
                        DropdownMenu(expanded = machineMenu, onDismissRequest = { machineMenu = false }) {
                            DropdownMenuItem(text = { Text("清除机台") }, onClick = {
                                state.db.updateFirstCureMachine(f.id, "")
                                refreshFirstCure(state); machineMenu = false
                            })
                            val enabledMachines = state.prefs.machines.ifEmpty { MachineList.machineIds }
                            enabledMachines.forEach { m ->
                                DropdownMenuItem(text = { Text(m) }, onClick = {
                                    state.db.updateFirstCureMachine(f.id, m)
                                    refreshFirstCure(state); machineMenu = false
                                })
                            }
                        }
                    }
                    OutlinedButton(onClick = {
                        state.db.deleteFirstCure(f.id); checked.remove(f.id)
                        refreshFirstCure(state); state.setStatus("已删除 1 条")
                    }) { Text("删除本条") }
                }
            }
        }
    }

    // 清空全部确认
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空全部首缸记录？") },
            text = { Text("将删除全部 ${state.firstCures.size} 条首缸记录（工艺数据、不良、备忘不受影响）。此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    state.db.clearFirstCure(); checked.clear(); refreshFirstCure(state)
                    state.setStatus("已清空全部首缸记录")
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }

    // 批量删除确认
    if (confirmDeleteSelected) {
        val selIds = filtered.filter { checked[it.id] == true }.map { it.id }
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("删除选中的 ${selIds.size} 条记录？") },
            text = { Text("此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSelected = false
                    selIds.forEach { state.db.deleteFirstCure(it); checked.remove(it) }
                    refreshFirstCure(state)
                    state.setStatus("已删除 ${selIds.size} 条记录")
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSelected = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MiniParam(label: String, value: String, modifier: Modifier = Modifier, suffix: String = "") {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        Text(
            value + suffix, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

/** 导出目录：prefs.exportDir 优先，未配置时用桌面。 */
internal fun exportDir(state: AppState): File {
    val configured = state.prefs.exportDir
    if (configured.isNotEmpty()) {
        val f = File(configured)
        if (f.exists() || f.mkdirs()) return f
    }
    return File(System.getProperty("user.home") ?: ".", "Desktop")
}

private fun openExportDir(state: AppState) {
    val dir = exportDir(state)
    dir.mkdirs()
    try {
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(dir)
        } else {
            ProcessBuilder("explorer.exe", dir.absolutePath).start()
        }
        state.setStatus("已打开导出目录：${dir.absolutePath}")
    } catch (_: Throwable) {
        try {
            ProcessBuilder("explorer.exe", dir.absolutePath).start()
        } catch (_: Throwable) {
            state.setStatus("无法打开目录：${dir.absolutePath}")
        }
    }
}

private fun exportSelected(state: AppState, sel: List<FirstCure>) {
    if (sel.isEmpty()) { state.setStatus("请先勾选要导出的记录"); return }
    if (sel.size > 300) { state.setStatus("一次最多导出 300 条，请分批"); return }
    try {
        val dir = exportDir(state)
        val file = File(dir, FirstCureExport.todayFileName())
        val img = FirstCureExport.render(sel, "硫化首缸清单", state.aliasLookup)
        if (FirstCureExport.save(img, file)) {
            AppLog.line("[首缸] 导出图片 ${sel.size} 条 → ${file.absolutePath}")
            state.setStatus("已导出 ${sel.size} 条 → ${file.absolutePath}")
        } else {
            state.setStatus("导出失败：写入图片出错")
        }
    } catch (e: Exception) {
        AppLog.line("[首缸] 导出异常: ${e.message}")
        state.setStatus("导出失败：${e.message}")
    }
}

private fun refreshFirstCure(state: AppState) {
    state.firstCures.clear(); state.firstCures.addAll(state.db.queryFirstCures())
}

// ---------------- 3. 不良 ----------------

/** 饼图色板（与网页版一致） */
private val PIE_COLORS = listOf(
    Color(0xFF1565C0), Color(0xFF26A69A), Color(0xFFEF6C00), Color(0xFF8E24AA),
    Color(0xFF43A047), Color(0xFFD81B60), Color(0xFF6D4C41), Color(0xFF546E7A)
)

/**
 * 外观不良：**左右分栏**布局。
 * 左侧为列表（固定宽度、独立滚动、点击不跳动），右侧为详情（独立滚动）。
 * 对齐网页版信息层次；并解决网页版未处理的问题：FM 家族子类（FMC/FMG/FMW…）
 * 在判胎表中独立成 code 但不良表只有 FM 一条，精确匹配会漏数据，故按前缀聚合。
 */
@Composable
fun DefectScreen(state: AppState) {
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<String?>(null) }
    var noteEdit by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        AppLog.line("[不良] 页面打开：共 ${state.defects.size} 条不良")
    }
    val filtered = if (search.isBlank()) state.defects
    else state.defects.filter { it.code.contains(search, true) || it.name.contains(search, true) }

    Row(modifier = Modifier.fillMaxSize()) {
        // ============ 左：列表（固定宽，不随详情跳动）============
        Column(
            modifier = Modifier
                .width(352.dp)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text("外观不良", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Box(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("搜索（简称或名称）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Box(Modifier.height(6.dp))
            Text(
                "共 ${state.defects.size} 条，显示 ${filtered.size} 条",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
            Box(Modifier.height(8.dp))

            val listScroll = rememberScrollState()
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(listScroll)) {
                if (filtered.isEmpty()) {
                    Text("没有匹配的不良", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
                filtered.take(500).forEachIndexed { idx, d ->
                    val sel = selected == d.code
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (sel) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                else Color.Transparent
                            )
                            .clickable {
                                if (sel) {
                                    selected = null
                                } else {
                                    selected = d.code
                                    noteEdit = d.note
                                    val fam = state.db.queryCausesFamily(d.code)
                                    AppLog.line(
                                        "[不良] 打开详情 ${d.code} ${d.name}｜判胎记录 ${fam.size} 条｜" +
                                                "子类 ${fam.map { it.code }.filter { it != d.code }.distinct().size} 种"
                                    )
                                }
                            }
                            .padding(vertical = 8.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            d.code, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(58.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            d.name, fontSize = 13.sp,
                            modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (d.note.isNotEmpty()) {
                            Text("✎", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (idx < filtered.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    }
                }
            }
        }

        // 竖向分隔线
        Box(
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
        )

        // ============ 右：详情（独立滚动）============
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            val detailScroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(detailScroll)
                    .padding(16.dp)
            ) {
                val d = selected?.let { code -> state.defects.firstOrNull { it.code == code } }
                if (d == null) {
                    Box(Modifier.fillMaxWidth().padding(top = 120.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "← 点击左侧不良查看详情",
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    DefectDetailCard(state, d, noteEdit, { noteEdit = it }, onClose = { selected = null })
                }
            }
        }
    }
}

@Composable
private fun DefectDetailCard(
    state: AppState,
    d: Defect,
    noteEdit: String,
    onNoteChange: (String) -> Unit,
    onClose: () -> Unit
) {
    // FM 家族聚合：FM → FM/FMC/FME/FMG/FMW…
    val causes = remember(d.code) { state.db.queryCausesFamily(d.code) }
    val total = causes.sumOf { it.count }
    val subCodes = causes.map { it.code }.filter { it != d.code }.distinct()

    Card2 {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(d.code, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
            Box(Modifier.width(8.dp))
            Text(d.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Box(Modifier.height(4.dp))
        Text(
            if (causes.isEmpty()) "暂无判胎数据（请在数据页导入判胎分析表）"
            else buildString {
                // 明确区分三个维度：原因记录条数 / 不良合计条数 / 子类种数
                append("判胎记录 ${causes.size} 条 · 不良合计 $total 条")
                if (subCodes.isNotEmpty()) {
                    append("（含 ${subCodes.size} 种子类：")
                    append(subCodes.take(8).joinToString("/"))
                    if (subCodes.size > 8) append("…")
                    append("）")
                }
            },
            fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
        )
        JudgeGuide.forCode(d.code)?.let {
            Box(Modifier.height(6.dp))
            Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
        if (d.description.isNotEmpty()) {
            Box(Modifier.height(8.dp))
            Text(d.description, fontSize = 13.sp)
        }

        if (causes.isNotEmpty()) {
            // 原因占比 Top8（横向条形）
            Box(Modifier.height(14.dp))
            Text("原因占比 Top8", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(Modifier.height(8.dp))
            val top = causes.take(8)
            val mx = top.firstOrNull()?.count?.coerceAtLeast(1) ?: 1
            top.forEach { c -> CauseBar(c.cause, c.count, mx) }

            // 责任科室占比（饼图 + 百分比图例）
            val byDept = linkedMapOf<String, Int>()
            causes.forEach { c ->
                val k = c.dept.ifEmpty { "—" }
                byDept[k] = (byDept[k] ?: 0) + c.count
            }
            val depts = byDept.entries.sortedByDescending { it.value }.map { it.key to it.value }
            Box(Modifier.height(16.dp))
            Text("责任科室占比", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(Modifier.height(4.dp))
            Text(
                "主要责任科室：${depts.firstOrNull()?.first ?: "—"}（占 ${pct(depts.firstOrNull()?.second ?: 0, total)}%）",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
            Box(Modifier.height(8.dp))
            val holeColor = MaterialTheme.colorScheme.surface
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(132.dp)) { drawDonut(depts, holeColor) }
                Box(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    depts.forEachIndexed { i, (k, v) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                Modifier.size(9.dp)
                                    .background(PIE_COLORS[i % PIE_COLORS.size], RoundedCornerShape(2.dp))
                            )
                            Box(Modifier.width(6.dp))
                            Text(
                                "$k  $v (${pct(v, total)}%)",
                                fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 判胎明细
            Box(Modifier.height(14.dp))
            Text("判胎明细（${causes.size} 条记录）", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(Modifier.height(4.dp))
            causes.take(60).forEach { c ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    if (c.code != d.code) {
                        Text(
                            "[${c.code}] ", fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.width(52.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Box(Modifier.width(52.dp))
                    }
                    Text(
                        c.cause.ifEmpty { "—" }, fontSize = 13.sp,
                        modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${c.dept.ifEmpty { "—" }} · ${c.count} 条",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1
                    )
                }
            }
        }

        // 我的备注
        Box(Modifier.height(14.dp))
        Text("我的备注（可编辑）", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(6.dp))
        OutlinedTextField(
            value = noteEdit, onValueChange = onNoteChange,
            modifier = Modifier.fillMaxWidth(), minLines = 2
        )
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                state.db.updateDefectNote(d.code, noteEdit)
                state.defects.clear(); state.defects.addAll(state.db.queryDefects())
                state.setStatus("备注已保存：${d.code}")
            }) { Text("保存备注") }
            OutlinedButton(onClick = {
                DesktopUtil.copyText(buildCopyText(d, causes))
                state.setStatus("已复制判胎信息（${causes.size} 条）")
            }) { Text("复制判胎信息") }
            OutlinedButton(onClick = onClose) { Text("收起") }
        }
    }
}

/** 横向占比条 */
@Composable
private fun CauseBar(label: String, count: Int, max: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)
    ) {
        Text(
            label.ifEmpty { "—" }, fontSize = 12.sp,
            modifier = Modifier.width(116.dp), maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Box(
            Modifier.weight(1f).height(16.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth((count.toFloat() / max).coerceIn(0.02f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            "${count}条", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(50.dp), textAlign = TextAlign.End, maxLines = 1
        )
    }
}

/** 甜甜圈图：按占比绘制扇区，中心用表面色挖空形成环。 */
private fun DrawScope.drawDonut(data: List<Pair<String, Int>>, holeColor: Color) {
    val total = data.sumOf { it.second }.coerceAtLeast(1)
    var start = -90f
    data.forEachIndexed { i, (_, v) ->
        val sweep = v * 360f / total
        drawArc(
            color = PIE_COLORS[i % PIE_COLORS.size],
            startAngle = start,
            sweepAngle = sweep,
            useCenter = true
        )
        start += sweep
    }
    drawCircle(color = holeColor, radius = size.minDimension / 2f * 0.45f)
}

private fun pct(part: Int, total: Int): String {
    val p = part * 100.0 / total.coerceAtLeast(1)
    return if (p == p.toLong().toDouble()) p.toLong().toString() else "%.1f".format(p)
}

private fun buildCopyText(d: Defect, causes: List<DefectCause>): String = buildString {
    append(d.code).append(' ').append(d.name).append('\n')
    if (d.note.isNotEmpty()) append(d.note).append('\n')
    causes.forEach { append("${it.cause} / ${it.dept} / ${it.count}条\n") }
}

// ---------------- 4. 备忘 ----------------

@Composable
fun MemoScreen(state: AppState) {
    SectionTitle("工作备忘录（${state.memos.size}）")
    var edit by remember { mutableStateOf("") }
    var editingId by remember { mutableStateOf<Long?>(null) }

    Card2 {
        OutlinedTextField(value = edit, onValueChange = { edit = it }, modifier = Modifier.fillMaxWidth())
        Box(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (edit.isBlank()) return@Button
                val id = editingId
                if (id == null) state.db.insertMemo(edit) else state.db.updateMemo(id, edit)
                edit = ""; editingId = null
                state.memos.clear(); state.memos.addAll(state.db.queryMemos())
                state.setStatus("已保存")
            }) { Text(if (editingId == null) "新增" else "保存修改") }
            if (editingId != null) {
                OutlinedButton(onClick = { edit = ""; editingId = null }) { Text("取消") }
            }
        }
    }

    state.memos.forEach { m ->
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(m.updatedAt))
        Card2 {
            Text(m.content, fontSize = 14.sp)
            Box(Modifier.height(4.dp))
            Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Box(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { edit = m.content; editingId = m.id }) { Text("编辑") }
                OutlinedButton(onClick = {
                    state.db.deleteMemo(m.id)
                    state.memos.clear(); state.memos.addAll(state.db.queryMemos())
                    state.setStatus("已删除")
                }) { Text("删除") }
            }
        }
    }
}

// ---------------- 5. 数据导入 ----------------

@Composable
fun ImportScreen(state: AppState) {
    SectionTitle("数据更新（从 Excel 导入）")
    Card2 {
        Text(
            "支持四种表自动识别：工艺标准 / 拉直高度 / 不良简称 / 判胎分析。\n" +
                    "导入会按类型替换旧数据；不良表的用户备注会自动保留。",
            fontSize = 13.sp
        )
        Box(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { importFlow(state, null) }) { Text("选择 Excel 导入") }
        }
        Box(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { importFlow(state, TableType.PROCESS) }) { Text("仅导入工艺标准") }
            OutlinedButton(onClick = { importFlow(state, TableType.HEIGHT) }) { Text("仅导入拉直高度") }
        }
        Box(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { importFlow(state, TableType.DEFECT) }) { Text("仅导入不良简称") }
            OutlinedButton(onClick = { importFlow(state, TableType.JUDGE) }) { Text("仅导入判胎分析") }
        }
    }

    Card2 {
        Text("当前数据统计", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        KvRow("花纹数", state.stats.patternCount.toString())
        KvRow("工艺条数", state.stats.specCount.toString())
        KvRow("拉直高度", state.stats.heightCount.toString())
        KvRow("不良简称", state.stats.defectCount.toString())
        KvRow("判胎分析", state.stats.judgeCount.toString())
        KvRow("首缸", state.stats.firstCureCount.toString())
        KvRow("备忘", state.stats.memoCount.toString())
        KvRow("上次导入", state.stats.lastImport)
    }
}

private fun importFlow(state: AppState, forced: TableType?) {
    val f = pickOpenFile("选择要导入的 Excel", "xlsx") ?: return
    try {
        val (detected, _) = ImportManager.peek(f)
        val stats = ImportManager.applyImport(state.db, f, forced)
        AppLog.line("导入 ${f.name} → 类型=${stats.type} 条数=${stats.rowsImported} sheet=${stats.sheetsFound}")
        state.setStatus(
            "导入完成：识别为【${stats.type}】${stats.rowsImported} 条" +
                    if (detected.label != stats.type) "（原识别 ${detected.label}）" else ""
        )
        state.reloadAll()
    } catch (e: Exception) {
        AppLog.line("导入失败: ${e.message}")
        state.setStatus("导入失败：${e.message}")
    }
}

// ---------------- 6. 备份 ----------------

@Composable
fun BackupScreen(state: AppState) {
    SectionTitle("数据备份 / 恢复")
    Card2 {
        Text(
            "导出为 JSON 数据包（工艺 / 拉直高度 / 不良及备注 / 判胎 / 首缸 / 备忘 / 设置），\n" +
                    "与手机版、网页版通用，可互相导入。",
            fontSize = 13.sp
        )
        Box(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { exportBackup(state) }) { Text("导出数据包(.json)") }
            OutlinedButton(onClick = { importBackup(state) }) { Text("导入数据包") }
        }
        Box(Modifier.height(8.dp))
        Text("数据目录：${com.heda.vulcan.data.DesktopDb.dataDir().absolutePath}", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline)
    }
}

private fun exportBackup(state: AppState) {
    val f = pickSaveFile("导出数据包", "vulcan-backup.json") ?: return
    try {
        val root = linkedMapOf<String, Any?>(
            "app" to "vulcan-desktop",
            "version" to 1,
            "exported_at" to ImportManager.nowText(),
            "prefs" to linkedMapOf<String, Any?>(
                "extra_seconds" to state.extraSeconds,
                "theme_id" to state.themeId,
                "dark_mode" to state.darkMode
            ),
            "process" to state.db.queryAllPatterns().flatMap { p ->
                state.db.querySizes(p).flatMap { sz -> state.db.querySpecs(p, sz) }
            }.map { s ->
                linkedMapOf<String, Any?>(
                    "pattern" to s.pattern, "material" to s.material, "size" to s.size, "region" to s.region,
                    "program" to s.program, "time_code" to s.timeCode, "capsule" to s.capsule, "inch" to s.inch,
                    "external_temp" to s.externalTemp, "clamp_force" to s.clampForce,
                    "pci_pressure" to s.pciPressure, "pci_height" to s.pciHeight, "rubber" to s.rubber,
                    "bead_code" to s.beadCode, "bead_angle" to s.beadAngle, "effective_date" to s.effectiveDate
                )
            },
            "defects" to state.defects.map {
                linkedMapOf<String, Any?>("code" to it.code, "name" to it.name, "description" to it.description, "note" to it.note)
            },
            "first_cure" to state.firstCures.map {
                linkedMapOf<String, Any?>(
                    "machine" to it.machine, "pattern" to it.pattern, "size" to it.size, "region" to it.region,
                    "time_code" to it.timeCode, "temp" to it.temp, "final_time" to it.finalTime,
                    "capsule" to it.capsule, "height" to it.height, "clamp_force" to it.clampForce,
                    "pci_pressure" to it.pciPressure, "pci_height" to it.pciHeight, "created_at" to it.createdAt
                )
            },
            "memos" to state.memos.map { linkedMapOf<String, Any?>("content" to it.content, "updated_at" to it.updatedAt) }
        )
        f.writeText(Json.write(root))
        state.setStatus("已导出：${f.absolutePath}")
    } catch (e: Exception) {
        state.setStatus("导出失败：${e.message}")
    }
}

private fun importBackup(state: AppState) {
    val f = pickOpenFile("选择数据包", "json") ?: return
    try {
        val root = Json.parse(f.readText()).asMap()
        val defects = root["defects"].asList().map {
            val m = it.asMap()
            com.heda.vulcan.data.Defect(
                m["code"].asStr(), m["name"].asStr(), m["description"].asStr(), m["note"].asStr()
            )
        }
        if (defects.isNotEmpty()) {
            state.db.clearDefect()
            state.db.insertDefectBatch(defects)
            // 恢复备注
            defects.forEach { if (it.note.isNotEmpty()) state.db.updateDefectNote(it.code, it.note) }
        }
        val memos = root["memos"].asList().map { it.asMap()["content"].asStr() }.filter { it.isNotEmpty() }
        if (memos.isNotEmpty()) memos.forEach { state.db.insertMemo(it) }

        state.reloadAll()
        state.setStatus("已导入数据包（不良 ${defects.size} 条、备忘 ${memos.size} 条）")
    } catch (e: Exception) {
        state.setStatus("导入失败：${e.message}")
    }
}

// ---------------- 7. 设置 ----------------

@Composable
fun SettingsScreen(state: AppState) {
    SectionTitle("设置")

    // ---- 机动时间 ----
    Card2 {
        Field("机动时间（秒）", state.extraSeconds.toString(), {
            it.toIntOrNull()?.let { v -> state.extraSeconds = v; state.persistPrefs() }
        }, Modifier.width(200.dp))
        Box(Modifier.height(4.dp))
        Text("最终硫化时间 = 代码时间 + 机动时间（默认 14 秒）", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline)
    }

    // ---- 主题外观（与 APK 同一套完整色板：背景色随主题整体变化）----
    Card2 {
        Text("主题外观", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Themes.all.forEach { t ->
                val sel = state.themeId == t.id
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.primary)
                            .clickable { state.themeId = t.id; state.persistPrefs() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sel) Text("✓", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Box(Modifier.height(4.dp))
                    Text(t.label, fontSize = 11.sp,
                        color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            }
        }
        Box(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.darkMode, onCheckedChange = { state.darkMode = it; state.persistPrefs() })
            Text("深色模式")
        }
    }

    // ---- 导出目录 ----
    Card2 {
        Text("首缸导出目录", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(6.dp))
        Text(exportDir(state).absolutePath, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val d = pickDirectory("选择首缸图片导出目录")
                if (d != null) {
                    state.prefs.exportDir = d.absolutePath
                    state.prefs.save()
                    state.setStatus("导出目录已设为：${d.absolutePath}")
                }
            }) { Text("更改目录") }
            OutlinedButton(onClick = { openExportDir(state) }) { Text("打开目录") }
            if (state.prefs.exportDir.isNotEmpty()) {
                OutlinedButton(onClick = {
                    state.prefs.exportDir = ""; state.prefs.save()
                    state.setStatus("已恢复默认导出目录（桌面）")
                }) { Text("恢复默认") }
            }
        }
        Box(Modifier.height(4.dp))
        Text("导出图片自动保存到该目录，文件名如「2026年10月10日首缸图.png」", fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline)
    }

    // ---- 胶囊对齐（花纹别名）----
    Card2 {
        var aliases by remember { mutableStateOf(state.prefs.capsuleAliases.map { it.toList() }) }
        var newKey by remember { mutableStateOf("") }
        var newVal by remember { mutableStateOf("") }

        Text("胶囊对齐（花纹别名）", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(4.dp))
        Text(
            "规格中的花纹与实际花纹不一致时配置对应关系（如 STEADY-33 对应 A2000）。" +
                    "识别到不认识的花纹会自动按此换算，并在提示中说明「某花纹对应某花纹」。忽略大小写。",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.outline
        )
        Box(Modifier.height(8.dp))

        if (aliases.isEmpty()) {
            Text("（暂无对齐规则）", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        }
        aliases.forEachIndexed { i, pair ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(pair[0], fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(150.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("→", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                Box(Modifier.width(10.dp))
                Text(pair.getOrElse(1) { "" }, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                TextButton(onClick = {
                    val nl = aliases.toMutableList().also { it.removeAt(i) }
                    aliases = nl.map { it.toList() }
                    state.prefs.capsuleAliases = aliases.toMutableList()
                    state.prefs.save()
                    state.setStatus("已删除对齐：${pair[0]} → ${pair.getOrElse(1) { "" }}")
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            }
        }

        Box(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = newKey, onValueChange = { newKey = it },
                label = { Text("关键词") }, singleLine = true,
                modifier = Modifier.width(170.dp)
            )
            Text("→", fontSize = 15.sp)
            OutlinedTextField(
                value = newVal, onValueChange = { newVal = it },
                label = { Text("对应花纹") }, singleLine = true,
                modifier = Modifier.width(170.dp)
            )
            Button(
                enabled = newKey.isNotBlank() && newVal.isNotBlank(),
                onClick = {
                    aliases = aliases + listOf(listOf(newKey.trim(), newVal.trim()))
                    state.prefs.capsuleAliases = aliases.toMutableList()
                    state.prefs.save()
                    state.setStatus("已添加对齐：${newKey.trim()} → ${newVal.trim()}")
                    newKey = ""; newVal = ""
                }
            ) { Text("添加") }
            OutlinedButton(onClick = {
                aliases = CapsuleAlias.defaultAliases().map { it.toList() }
                state.prefs.capsuleAliases = aliases.toMutableList()
                state.prefs.save()
                state.setStatus("已恢复默认三条对齐")
            }) { Text("恢复默认") }
        }
    }

    // ---- 机台（仅用于首缸展示）----
    Card2 {
        var machines by remember { mutableStateOf(state.prefs.machines.toList()) }
        val allIds = MachineList.machineIds
        Text("机台（仅用于首缸展示）· 已启用 ${machines.size} 台", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                machines = allIds.toList()
                state.prefs.machines = machines.toMutableList(); state.prefs.save()
            }) { Text("全选") }
            OutlinedButton(onClick = {
                machines = listOf("1101", "1102", "1103", "1104")
                state.prefs.machines = machines.toMutableList(); state.prefs.save()
            }) { Text("最少（只留 1101-1104）") }
            OutlinedButton(onClick = {
                machines = emptyList()
                state.prefs.machines = mutableListOf(); state.prefs.save()
            }) { Text("清空") }
        }
        Box(Modifier.height(8.dp))
        allIds.chunked(8).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { id ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = id in machines,
                            onCheckedChange = { on ->
                                machines = if (on) machines + id else machines - id
                                state.prefs.machines = machines.toMutableList(); state.prefs.save()
                            }
                        )
                        Text(id, fontSize = 12.sp)
                    }
                }
                repeat(8 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }

    // ---- 托盘与快捷键 ----
    Card2 {
        Text("系统与快捷键", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.trayEnabled, onCheckedChange = { state.trayEnabled = it; state.persistPrefs() })
            Text("启用系统托盘")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.closeToTray, onCheckedChange = { state.closeToTray = it; state.persistPrefs() })
            Text("关闭窗口时最小化到托盘")
        }
        KvRow("截图热键", state.prefs.screenshotHotkey)
        KvRow("全局搜索", state.prefs.searchTrigger)
    }

    Card2 {
        Text("数据统计", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        KvRow("花纹 / 工艺", "${state.stats.patternCount} / ${state.stats.specCount}")
        KvRow("拉直高度", state.stats.heightCount.toString())
        KvRow("不良 / 判胎", "${state.stats.defectCount} / ${state.stats.judgeCount}")
        KvRow("首缸 / 备忘", "${state.stats.firstCureCount} / ${state.stats.memoCount}")
        KvRow("上次导入", state.stats.lastImport)
        Box(Modifier.height(8.dp))
        KvRow("数据目录", com.heda.vulcan.data.DesktopDb.dataDir().absolutePath)
    }
}

// ---------------- 8. 在线更新 ----------------

@Composable
fun UpdateScreen(state: AppState) {
    SectionTitle("在线更新")
    val scope = rememberCoroutineScope()
    var manifest by remember { mutableStateOf<Updater.Manifest?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var readyDir by remember { mutableStateOf<File?>(null) }

    Card2 {
        KvRow("当前版本", "v${AppVersion.VERSION}（构建 ${AppVersion.VERSION_CODE}）")
        Box(Modifier.height(8.dp))
        Button(onClick = {
            if (checking) return@Button
            checking = true
            state.setStatus("正在检查更新…")
            scope.launch(Dispatchers.IO) {
                val m = Updater.fetch()   // 多源回退：GitHub API → raw → CDN
                withContext(Dispatchers.Main) {
                    checking = false
                    manifest = m
                    state.setStatus(
                        when {
                            m == null -> "检查更新失败：请确认网络连通"
                            Updater.hasUpdate(m) -> "发现新版本 v${m.desktopRebuild.version}"
                            else -> "当前已是最新版本"
                        }
                    )
                }
            }
        }, enabled = !checking) { Text(if (checking) "检查中…" else "检查更新") }
    }

    manifest?.let { m ->
        Card2 {
            Text("最新版本：v${m.version}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            if (m.notes.isNotEmpty()) {
                Box(Modifier.height(6.dp))
                Text(m.notes, fontSize = 13.sp)
            }
        }

        // 通道一：重建编译版
        if (m.desktopRebuild.available) {
            UpdateAssetCard(
                title = "重建编译版（推荐）",
                desc = "有源码可维护的版本 v${m.desktopRebuild.version}",
                asset = m.desktopRebuild,
                downloading = downloading,
                progress = progress,
                onDownload = { asset -> downloadAsset(scope, state, asset, { downloading = it }, { progress = it }, { readyDir = it }) }
            )
        }

        // 通道二：原版程序包
        if (m.desktopOriginal.available) {
            UpdateAssetCard(
                title = "原版程序包",
                desc = "原始版本 v${m.desktopOriginal.version}（无源码，作为基线保留）",
                asset = m.desktopOriginal,
                downloading = downloading,
                progress = progress,
                onDownload = { asset -> downloadAsset(scope, state, asset, { downloading = it }, { progress = it }, { readyDir = it }) }
            )
        }
    }

    if (downloading) {
        Card2 {
            Text("下载中：$progress%", fontSize = 13.sp)
        }
    }

    readyDir?.let { dir ->
        Card2 {
            Text("新版已就绪", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(Modifier.height(6.dp))
            Text(dir.absolutePath, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
            Box(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                try { java.awt.Desktop.getDesktop().open(dir) } catch (_: Exception) {}
            }) { Text("打开目录") }
            Box(Modifier.height(6.dp))
            Text(
                "请关闭本程序后，运行目录内的启动程序完成切换。",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun UpdateAssetCard(
    title: String,
    desc: String,
    asset: Updater.Asset,
    downloading: Boolean,
    progress: Int,
    onDownload: (Updater.Asset) -> Unit
) {
    Card2 {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Box(Modifier.height(4.dp))
        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
        Box(Modifier.height(8.dp))
        Button(onClick = { onDownload(asset) }, enabled = !downloading) {
            Text(if (downloading) "下载中 $progress%" else "下载 v${asset.version}")
        }
    }
}

private fun downloadAsset(
    scope: kotlinx.coroutines.CoroutineScope,
    state: AppState,
    asset: Updater.Asset,
    setDownloading: (Boolean) -> Unit,
    setProgress: (Int) -> Unit,
    setReady: (File?) -> Unit
) {
    scope.launch(Dispatchers.IO) {
        setDownloading(true)
        setProgress(0)
        val dir = Updater.updateDir(asset.version)
        val zip = File(dir, "update.zip")
        val ok = Updater.download(asset, zip) { p -> setProgress(p) }
        val verified = ok && Updater.verifySha256(zip, asset.sha256)
        val done = verified && Updater.unzip(zip, dir)
        zip.delete()
        setDownloading(false)
        val msg = if (done) "已下载并解压到：${dir.absolutePath}" else "下载失败或校验未通过"
        state.setStatus(msg)
        setReady(if (done) dir else null)
    }
}

/** 占位：OCR / 全局热键 / 托盘在 Main.kt 中挂载（见 core 包）。 */
@Suppress("unused")
private fun unusedPlaceholder() {}
