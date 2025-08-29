package org.example.com.yongfeng.aps

import com.yongfeng.aps.ExcelReader
import com.yongfeng.aps.PieceStep
import com.yongfeng.aps.ScheduleOptimizer

// Main.kt
fun main() {

    val excelReader = ExcelReader()
    val pieces = excelReader.readExcel("数据模板.xlsx").toMutableList()

    val optimizer = ScheduleOptimizer()
    val optimizedPieces = optimizer.optimalSequenceForMainPart(pieces)

    // 输出优化结果
    println("原始顺序的厚度序列：")
    pieces.forEach { println("${it.sequence}: ${it.thickness}") }

    println("\n优化后的厚度序列：")
    optimizedPieces.forEach { println("${it.sequence}: ${it.thickness}") }

    // 计算并输出优化效果
    fun calculateAverageThicknessDiff(list: List<PieceStep>): Double {
        return list.zipWithNext { a, b -> Math.abs(a.thickness - b.thickness) }
            .average()
    }

    println("\n优化效果：")
    println("原始平均厚度差：${calculateAverageThicknessDiff(pieces)}")
    println("优化后平均厚度差：${calculateAverageThicknessDiff(optimizedPieces)}")
}