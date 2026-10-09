package com.heda.vulcan.data

/**
 * 机台清单（桌面版特有，来自反编译还原）。
 * 分组 11/12/13/14 × 01..22 → 1101..1422，共 88 台。
 */
object MachineList {

    data class Machine(val id: String, val label: String)

    val all: List<Machine> by lazy {
        val out = mutableListOf<Machine>()
        for (group in listOf("11", "12", "13", "14")) {
            for (n in 1..22) {
                val s = "%02d".format(n)
                out.add(Machine(group + s, group + s))
            }
        }
        out
    }

    val machineIds: List<String> get() = all.map { it.id }
}
