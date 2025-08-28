package org.example.com.yongfeng.aps

import com.yongfeng.aps.ExcelReader
import com.yongfeng.aps.PieceStep

// ScheduleOptimizer.kt
class ScheduleOptimizer {
    /**
     * 同钢种的块间调序
     * 将厚度小的放在前面的块里
     */
    /**
     * 同钢种的块间调序，将厚度小的放在前面的块里
     */
    fun adjustForThinFirst(original: List<List<PieceStep>>): List<List<PieceStep>> {
        // 按钢种分组并排序
        val groupedBySteelGrade = original
            .flatMap { it }
            .groupBy { it.innerSteelGrade }
            .mapValues { (_, pieceSteps) -> pieceSteps.sortedBy { it.thickness } }

        // 创建每个钢种的迭代器
        val iterators = groupedBySteelGrade.mapValues { (_, pieceSteps) -> pieceSteps.iterator() }

        // 重新构建结果
        return original.map { originalGroup ->
            val steelGrade = originalGroup.firstOrNull()?.innerSteelGrade ?: return@map emptyList()
            val iterator = iterators[steelGrade] ?: return@map emptyList()

            // 从排序后的列表中取出与原组相同数量的元素
            (0 until originalGroup.size)
                .mapNotNull { if (iterator.hasNext()) iterator.next() else null }
                .toList()
        }
    }

    /**
     * 初始顺序是根据浇次计划排产得到，优化顺序以满足
     * 1.缓冷批
     * 2.规格跳变的平滑
     */
    fun optimalSequenceForMainPart(mainPartToPlan: MutableList<PieceStep>): MutableList<PieceStep> {
        var result = mutableListOf<PieceStep>()
        val groupInnerSteelGradeList = adjustForThinFirst(splitByInnerSteelGradeChange(mainPartToPlan))
        result = ScheduleOptimizer().optimizeThicknessTransition(groupInnerSteelGradeList).toMutableList()
        return result
    }

    /**
     * 通过内部钢种变化按序拆分为多个列表
     * 不能groupBy(it.innerSteelGrade)因为一种innerSteelGrade可能被分隔在多块
     */
    fun splitByInnerSteelGradeChange(mainPartToPlan: List<PieceStep>): List<List<PieceStep>> {
        if (mainPartToPlan.isEmpty()) return emptyList()

        val result = mutableListOf<List<PieceStep>>()
        var currentGroup = mutableListOf<PieceStep>()
        var currentGrade = mainPartToPlan.first().innerSteelGrade

        for (piece in mainPartToPlan) {
            if (piece.innerSteelGrade == currentGrade) {
                currentGroup.add(piece)
            } else {
                result.add(currentGroup.toList())
                currentGroup = mutableListOf(piece)
                currentGrade = piece.innerSteelGrade
            }
        }
        result.add(currentGroup) // 添加最后一个分组

        return result
    }

    /**
     * 根据List<List<PieceStep>>扁平化
     * 1.只调整List<PieceStep>内元素的顺序，元素不可以跨list
     * 2.目的使得扁平化的结果两两相邻的卷厚度尽可能平滑
     */
    fun optimizeThicknessTransition(allList: List<List<PieceStep>>): List<PieceStep> {
        // 1. 首先计算每个子列表正序和逆序的两种可能性
        data class GroupState(
            val pieces: List<PieceStep>,
            val isReversed: Boolean,
            val firstThickness: Double,
            val lastThickness: Double
        )

        // 2. 对每个子列表生成两种状态
        val groupStates = allList.map { subList ->
            listOf(
                GroupState(
                    subList,
                    false,
                    subList.first().thickness,
                    subList.last().thickness
                ),
                GroupState(
                    subList.reversed(),
                    true,
                    subList.last().thickness,
                    subList.first().thickness
                )
            )
        }

        // 3. 动态规划求解
        data class DPState(
            val totalDiff: Double,  // 当前累积的厚度差平方和
            val selectedStates: List<GroupState>  // 选择的状态列表
        )

        fun calculateDiff(thick1: Double, thick2: Double): Double {
            return (thick1 - thick2) * (thick1 - thick2)
        }

        var dpStates = mapOf(0 to DPState(0.0, emptyList()))

        // 4. 遍历每个组，构建最优解
        for (i in groupStates.indices) {
            val newDpStates = mutableMapOf<Int, DPState>()

            for ((_, currentState) in dpStates) {
                for (groupState in groupStates[i]) {
                    val newDiff = if (currentState.selectedStates.isEmpty()) {
                        0.0
                    } else {
                        calculateDiff(
                            currentState.selectedStates.last().lastThickness,
                            groupState.firstThickness
                        )
                    }

                    val totalDiff = currentState.totalDiff + newDiff
                    val newSelectedStates = currentState.selectedStates + groupState

                    val key = i + 1
                    if (!newDpStates.containsKey(key) ||
                        newDpStates[key]!!.totalDiff > totalDiff
                    ) {
                        newDpStates[key] = DPState(totalDiff, newSelectedStates)
                    }
                }
            }
            dpStates = newDpStates
        }

        // 5. 构建最终结果
        val finalState = dpStates[groupStates.size]!!
        return finalState.selectedStates.flatMap { it.pieces }
    }
}

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