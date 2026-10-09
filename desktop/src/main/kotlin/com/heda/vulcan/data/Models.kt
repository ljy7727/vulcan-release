package com.heda.vulcan.data

/**
 * 领域模型（与安卓端 Models.kt 保持一致，桌面版另加若干桌面专用模型）。
 * 说明：本文件由反编译产物 + 安卓端源码对照还原。
 */

/** 一条工艺规格记录（来自"工艺标准"表，sheet 名 = 花纹） */
data class ProcessSpec(
    val pattern: String,        // 花纹（sheet 名）
    val material: String,       // 完整物料描述
    val size: String,           // 尺寸（物料第一个词）
    val region: String,         // 销售区域 CH/EU/CN
    val program: String,        // 硫化程序
    val timeCode: String,       // 硫化时间代码，如 PB178T10
    val capsule: String,        // 胶囊规格
    val inch: String,           // 英寸
    val externalTemp: String,   // 外温
    val clampForce: String,     // 合模力(KN)
    val pciPressure: String,    // PCI压力(Mpa)
    val pciHeight: String,      // PCI高度(mm)
    val rubber: String,         // 胶种
    val beadCode: String,       // 钢圈代码
    val beadAngle: String,      // 钢圈角度
    val effectiveDate: String   // 生效日期
)

/** 胶囊拉直高度 */
data class CapsuleHeight(
    val capsule: String,
    val manufacturer: String,
    val height: String,      // 拉直高度
    val perimeter: String,   // 断面周长
    val diff: String
)

/** 外观不良简称 */
data class Defect(
    val code: String,
    val name: String,
    val description: String,
    val note: String = ""    // 用户自定义备注
)

/** 判胎信息：不良代码对应的原因分析 / 责任科室 / 条数 */
data class DefectCause(
    val code: String,
    val name: String,
    val cause: String,
    val dept: String,
    val count: Int
)

/** 首缸记录 */
data class FirstCure(
    val id: Long,
    val machine: String,
    val pattern: String,
    val material: String,
    val size: String,
    val region: String,
    val timeCode: String,
    val temp: String,
    val finalTime: String,
    val capsule: String,
    val height: String,
    val clampForce: String,
    val pciPressure: String,
    val pciHeight: String,
    val extra: String,
    val createdAt: Long
)

/** 备忘录 */
data class Memo(
    val id: Long,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)

/** 导入统计结果 */
data class ImportStats(
    val type: String = "",
    val sheetsFound: Int = 0,
    val rowsImported: Int = 0,
    val rowsSkippedEmpty: Int = 0,
    val hiddenRowsSkipped: Int = 0,
    val hiddenColsSkipped: Int = 0,
    val sheetNames: List<String> = emptyList(),
    val message: String = ""
)

/** 数据库整体统计 */
data class DbStats(
    val patternCount: Int = 0,
    val specCount: Int = 0,
    val heightCount: Int = 0,
    val defectCount: Int = 0,
    val judgeCount: Int = 0,
    val firstCureCount: Int = 0,
    val memoCount: Int = 0,
    val lastImport: String = ""
)

/** 电子表格类型（自动识别） */
enum class TableType(val label: String) {
    PROCESS("工艺标准"),
    HEIGHT("拉直高度"),
    DEFECT("不良简称"),
    JUDGE("判胎分析"),
    UNKNOWN("无法识别")
}
