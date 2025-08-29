package com.yongfeng.aps

import ilog.concert.*
import ilog.cplex.*

class ScheduleOptimizer {

    /**
     * 优化厚度过渡的主方法
     */
    fun optimizeThicknessTransition(allList: List<List<PieceStep>>): List<PieceStep> {
        if (allList.isEmpty()) return emptyList()
        if (allList.size == 1) return optimizeGroupInternal(allList[0], null, null)

        // 步骤1：使用CPLEX确定每组的开始卷和结束卷
        val groupSelections = solveGroupSelectionGreedy(allList)

        // 步骤2：基于确定的开始结束卷，优化每组内部顺序
        val result = mutableListOf<PieceStep>()
        for (i in allList.indices) {
            val group = allList[i]
            val selection = groupSelections[i]
            val prevEnd = if (i == 0) createVirtualStart(group) else groupSelections[i-1].endPiece
            val nextStart = if (i == allList.size - 1) null else groupSelections[i+1].startPiece

            val optimizedGroup = optimizeGroupInternal(group, selection.startPiece, selection.endPiece)
            result.addAll(optimizedGroup)
        }

        return result
    }
//    /**
//     * 简化版的CPLEX建模 - 只考虑相邻组间的直接过渡代价
//     */
//    private fun solveGroupSelectionWithCPLEX(allList: List<List<PieceStep>>): List<GroupSelection> {
//        try {
//            val cplex = IloCplex()
//            cplex.setParam(IloCplex.Param.MIP.Display, 0)
//            cplex.setParam(IloCplex.Param.TimeLimit, 60.0)
//
//            val n = allList.size
//            val groupSizes = allList.map { it.size }
//
//            // 决策变量：x[i][j][k] 表示第i组选择第j个元素作为开始，第k个元素作为结束
//            val x = Array(n) { i ->
//                Array(groupSizes[i]) { j ->
//                    Array(groupSizes[i]) { k ->
//                        cplex.boolVar("x_${i}_${j}_${k}")
//                    }
//                }
//            }
//
//            // 约束：每组必须选择一个开始卷和一个结束卷
//            for (i in 0 until n) {
//                val expr = cplex.linearNumExpr()
//                for (j in 0 until groupSizes[i]) {
//                    for (k in 0 until groupSizes[i]) {
//                        expr.addTerm(1.0, x[i][j][k])
//                    }
//                }
//                cplex.addEq(expr, 1.0, "group_${i}_selection")
//            }
//
//            // 目标函数：最小化过渡代价
//            val objective = cplex.linearNumExpr()
//
//            // 直接计算所有可能的组间过渡代价
//            for (i in 0 until n - 1) {
//                for (j1 in 0 until groupSizes[i]) {
//                    for (k1 in 0 until groupSizes[i]) {
//                        val endPiece = allList[i][k1]
//                        for (j2 in 0 until groupSizes[i + 1]) {
//                            for (k2 in 0 until groupSizes[i + 1]) {
//                                val startPiece = allList[i + 1][j2]
//                                val cost = calculateTransitionCost(endPiece, startPiece)
//
//                                // 创建表示两个选择同时发生的二次项
//                                val product = cplex.prod(x[i][j1][k1], x[i + 1][j2][k2])
//                                objective.addTerm(cost, product)
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 添加第一组的虚拟开始代价
//            val virtualStart = createVirtualStart(allList[0])
//            for (j in 0 until groupSizes[0]) {
//                for (k in 0 until groupSizes[0]) {
//                    val startPiece = allList[0][j]
//                    val cost = calculateTransitionCost(virtualStart, startPiece)
//                    objective.addTerm(cost, x[0][j][k])
//                }
//            }
//
//            cplex.addMinimize(objective)
//
//            if (cplex.solve()) {
//                val result = mutableListOf<GroupSelection>()
//                for (i in 0 until n) {
//                    for (j in 0 until groupSizes[i]) {
//                        for (k in 0 until groupSizes[i]) {
//                            if (cplex.getValue(x[i][j][k]) > 0.5) {
//                                result.add(GroupSelection(allList[i][j], allList[i][k]))
//                                break
//                            }
//                        }
//                    }
//                }
//                cplex.end()
//                return result
//            } else {
//                cplex.end()
//                return solveGroupSelectionGreedy(allList)
//            }
//        } catch (e: Exception) {
//            println("CPLEX求解失败，使用贪心算法: ${e.message}")
//            return solveGroupSelectionGreedy(allList)
//        }
//    }

    /**
     * 贪心算法作为CPLEX的备选方案
     */
    private fun solveGroupSelectionGreedy(allList: List<List<PieceStep>>): List<GroupSelection> {
        val result = mutableListOf<GroupSelection>()
        var lastEnd: PieceStep? = null

        for (i in allList.indices) {
            val group = allList[i]
            val currentStart = if (i == 0) createVirtualStart(group) else lastEnd!!

            var bestSelection: GroupSelection? = null
            var bestCost = Double.MAX_VALUE

            for (start in group) {
                for (end in group) {
                    val startCost = calculateTransitionCost(currentStart, start)
                    val endCost = if (i < allList.size - 1) {
                        // 估算与下一组的最小过渡代价
                        allList[i + 1].minOfOrNull { calculateTransitionCost(end, it) } ?: 0.0
                    } else 0.0

                    val totalCost = startCost + endCost
                    if (totalCost < bestCost) {
                        bestCost = totalCost
                        bestSelection = GroupSelection(start, end)
                    }
                }
            }

            result.add(bestSelection!!)
            lastEnd = bestSelection.endPiece
        }

        return result
    }

    /**
     * 计算两个卷之间的过渡代价
     */
    private fun calculateTransitionCost(from: PieceStep, to: PieceStep): Double {
        val thicknessDiff = Math.abs(from.thickness - to.thickness)

        // 厚度过渡代价
        val thicknessCost = when {
            thicknessDiff <= 0.001 -> 0.0 // 基本相同
            to.thickness in from.minThickForNext..from.maxThickForNext -> thicknessDiff * 1.0 // 第一级范围
            to.thickness in from.nextMinThick..from.nextMaxThick -> thicknessDiff * 5.0 // 第二级范围
            else -> thicknessDiff * 20.0 + 100.0 // 超出范围，重型惩罚
        }

        // 属性变化代价
        var changeCost = 0.0
        if (from.width != to.width) changeCost += 10.0
        if (from.innerSteelGrade != to.innerSteelGrade) changeCost += 50.0

        return thicknessCost + changeCost
    }

    /**
     * 创建虚拟开始卷
     */
    private fun createVirtualStart(firstGroup: List<PieceStep>): PieceStep {
        val virtual = PieceStep()
        virtual.thickness = 5.75
        virtual.innerSteelGrade = "Q355B-1"
        virtual.steelGrade = "Q355B-1"
        virtual.width = firstGroup.firstOrNull()?.width ?: 1800.0
        virtual.minThickForNext = 0.0
        virtual.maxThickForNext = 50.0
        virtual.nextMinThick = 0.0
        virtual.nextMaxThick = 100.0
        return virtual
    }

    /**
     * 优化组内顺序（VRP问题）
     */
    private fun optimizeGroupInternal(
        group: List<PieceStep>,
        startPiece: PieceStep?,
        endPiece: PieceStep?
    ): List<PieceStep> {
        if (group.size <= 2) return group

        // 如果没有指定开始和结束，使用动态规划
        if (startPiece == null || endPiece == null) {
            return optimizeGroupDP(group)
        }

        // 确定开始和结束后，使用最近邻算法求解TSP
        return solveTSPWithFixedEndpoints(group, startPiece, endPiece)
    }

    /**
     * 使用动态规划优化组内顺序（原有方法的简化版）
     */
    private fun optimizeGroupDP(group: List<PieceStep>): List<PieceStep> {
        val sorted = group.sortedBy { it.thickness }
        val reversed = sorted.reversed()

        // 选择厚度变化更平滑的排列
        val sortedDiff = calculateTotalThicknessDiff(sorted)
        val reversedDiff = calculateTotalThicknessDiff(reversed)

        return if (sortedDiff <= reversedDiff) sorted else reversed
    }

    /**
     * 求解带固定端点的TSP问题
     */
    private fun solveTSPWithFixedEndpoints(
        group: List<PieceStep>,
        start: PieceStep,
        end: PieceStep
    ): List<PieceStep> {
        val remaining = group.filter { it != start && it != end }.toMutableList()
        val result = mutableListOf<PieceStep>()
        result.add(start)

        var current = start
        while (remaining.isNotEmpty()) {
            val next = remaining.minByOrNull { calculateTransitionCost(current, it) }!!
            remaining.remove(next)
            result.add(next)
            current = next
        }

        if (end != start) {
            result.add(end)
        }

        return result
    }

    /**
     * 计算序列的总厚度差
     */
    private fun calculateTotalThicknessDiff(pieces: List<PieceStep>): Double {
        if (pieces.size < 2) return 0.0
        return pieces.zipWithNext { a, b ->
            Math.abs(a.thickness - b.thickness)
        }.sum()
    }

    // 保持原有的其他方法不变
    fun adjustForThinFirst(original: List<List<PieceStep>>): List<List<PieceStep>> {
        val groupedBySteelGrade = original
            .flatMap { it }
            .groupBy { it.innerSteelGrade }
            .mapValues { (_, pieceSteps) -> pieceSteps.sortedBy { it.thickness } }

        val iterators = groupedBySteelGrade.mapValues { (_, pieceSteps) -> pieceSteps.iterator() }

        return original.map { originalGroup ->
            val steelGrade = originalGroup.firstOrNull()?.innerSteelGrade ?: return@map emptyList()
            val iterator = iterators[steelGrade] ?: return@map emptyList()

            (0 until originalGroup.size)
                .mapNotNull { if (iterator.hasNext()) iterator.next() else null }
                .toList()
        }
    }

    fun optimalSequenceForMainPart(mainPartToPlan: MutableList<PieceStep>): MutableList<PieceStep> {
        val groupInnerSteelGradeList = adjustForThinFirst(splitByInnerSteelGradeChange(mainPartToPlan))
        return optimizeThicknessTransition(groupInnerSteelGradeList).toMutableList()
    }

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
        result.add(currentGroup)

        return result
    }
}

/**
 * 表示每组的选择结果
 */
data class GroupSelection(
    val startPiece: PieceStep,
    val endPiece: PieceStep
)