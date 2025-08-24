package org.example.com.yongfeng.aps

import java.io.File
// ExcelReader.kt
import org.apache.poi.ss.usermodel.WorkbookFactory

// PieceStep.kt
data class PieceStep(
    val assignmentId: String,
    val sequence: Int,
    val plateWidth: Double,
    val width: Double,
    val thickness: Double,
    val internalSteelGrade: String,
    val steelGrade: String
)


class ExcelReader {
    fun readExcel(filePath: String): List<PieceStep> {
        val pieces = mutableListOf<PieceStep>()
        val workbook = WorkbookFactory.create(File(filePath))
        val sheet = workbook.getSheetAt(0)

        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex)
            pieces.add(PieceStep(
                assignmentId = row.getCell(0).stringCellValue,
                sequence = row.getCell(1).numericCellValue.toInt(),
                plateWidth = row.getCell(2).numericCellValue,
                width = row.getCell(3).numericCellValue,
                thickness = row.getCell(4).numericCellValue,
                internalSteelGrade = row.getCell(5).stringCellValue,
                steelGrade = row.getCell(6).stringCellValue
            ))
        }
        workbook.close()
        return pieces
    }
}

// ScheduleOptimizer.kt
class ScheduleOptimizer {
    // 计算相邻厚度差的评分
    private fun calculateThicknessScore(list: List<PieceStep>): Double {
        return list.zipWithNext { a, b ->
            Math.abs(a.thickness - b.thickness)
        }.sum()
    }

    // 计算宽度和钢种集中度评分
    private fun calculateGroupScore(list: List<PieceStep>): Double {
        return list.zipWithNext { a, b ->
            if (a.width == b.width && a.steelGrade == b.steelGrade) 1.0 else 0.0
        }.sum()
    }

    // 计算总评分(越小越好)
    private fun calculateTotalScore(list: List<PieceStep>): Double {
        val thicknessScore = calculateThicknessScore(list)
        val groupScore = -calculateGroupScore(list)
        return thicknessScore * 0.7 + groupScore * 0.3
    }

    fun optimize(pieces: List<PieceStep>): List<PieceStep> {
        // 第一步：分离薄规格
        val thinPieces = pieces.filter { it.thickness < 5.0 }
        val normalPieces = pieces.filter { it.thickness >= 5.0 }

        // 第二步：对normalPieces进行模拟退火优化
        val result = simulatedAnnealing(normalPieces)

        // 合并结果
        return thinPieces + result
    }

    private fun simulatedAnnealing(pieces: List<PieceStep>): List<PieceStep> {
        var currentSolution = pieces.toMutableList()
        var bestSolution = currentSolution.toMutableList()
        var bestScore = calculateTotalScore(bestSolution)

        var temperature = 100.0
        val coolingRate = 0.995
        val random = java.util.Random()

        while (temperature > 0.1) {
            val i = random.nextInt(pieces.size)
            val j = random.nextInt(pieces.size)

            val newSolution = currentSolution.toMutableList()
            val temp = newSolution[i]
            newSolution[i] = newSolution[j]
            newSolution[j] = temp

            val currentScore = calculateTotalScore(currentSolution)
            val newScore = calculateTotalScore(newSolution)

            if (newScore < currentScore ||
                random.nextDouble() < Math.exp((currentScore - newScore) / temperature)) {
                currentSolution = newSolution
                if (newScore < bestScore) {
                    bestScore = newScore
                    bestSolution = newSolution
                }
            }

            temperature *= coolingRate
        }

        return bestSolution
    }
}

// Main.kt
fun main() {
    val excelReader = ExcelReader()
    val pieces = excelReader.readExcel("厚度要平滑过渡.xlsx")

    val optimizer = ScheduleOptimizer()
    val optimizedPieces = optimizer.optimize(pieces)

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