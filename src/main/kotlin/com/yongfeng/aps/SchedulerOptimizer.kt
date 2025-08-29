package com.yongfeng.aps

import kotlin.math.abs

class ScheduleOptimizer {
    companion object {
        const val PENALTY_WEIGHT = 1000.0 // 属性变化的惩罚权重
        const val THIN_THRESHOLD = 7.75 // 薄卷阈值
    }

    /**
     * 优化厚度过渡的两阶段方法
     */
    fun optimizeThicknessTransition(allList: List<List<PieceStep>>): List<PieceStep> {
        // 第一阶段：确定每组的开始和结束卷
        val groupEndpoints = determineGroupEndpoints(allList)

        // 第二阶段：优化每组内的顺序
        val optimizedGroups = allList.mapIndexed { index, group ->
            optimizeGroupSequence(
                group,
                groupEndpoints[index].first,
                groupEndpoints[index].second
            )
        }

        return optimizedGroups.flatten()
    }

    /**
     * 第一阶段：确定每组的开始和结束卷
     */
    private fun determineGroupEndpoints(allList: List<List<PieceStep>>): List<Pair<PieceStep, PieceStep>> {
        val result = mutableListOf<Pair<PieceStep, PieceStep>>()

        // 特殊处理第一组，考虑与特定规格的衔接
        val firstGroupEndpoints = determineFirstGroupEndpoints(allList.first())
        result.add(firstGroupEndpoints)

        // 处理后续组
        for (i in 1 until allList.size) {
            val previousGroup = allList[i - 1]
            val currentGroup = allList[i]
            val previousEndpoint = result.last().second

            // 找到与前一组的结束卷过渡最好的开始和结束卷
            val endpoints = findBestEndpointsForGroup(currentGroup, previousEndpoint)
            result.add(endpoints)
        }

        return result
    }

    /**
     * 确定第一组的开始和结束卷，考虑与特定规格的衔接
     */
    private fun determineFirstGroupEndpoints(firstGroup: List<PieceStep>): Pair<PieceStep, PieceStep> {
        val referencePiece = createReferencePiece(firstGroup.first().width)

        // 评估所有可能的开始卷
        val startCandidates = firstGroup.map { piece ->
            piece to calculateTransitionCost(referencePiece, piece)
        }.sortedBy { it.second }

        // 选择最佳开始卷
        val bestStart = startCandidates.first().first

        // 选择与最佳开始卷配合最好的结束卷
        val endCandidates = firstGroup.filter { it != bestStart }.map { piece ->
            piece to calculateGroupInternalCost(firstGroup, bestStart, piece)
        }.sortedBy { it.second }

        return if (endCandidates.isEmpty()) {
            bestStart to bestStart // 只有一卷的情况
        } else {
            bestStart to endCandidates.first().first
        }
    }

    /**
     * 创建参考规格的虚拟卷
     */
    private fun createReferencePiece(width: Double): PieceStep {
        return PieceStep().apply {
            this.thickness = 5.75
            this.innerSteelGrade = "Q355B-1"
            this.width = width
            this.steelGrade = "Q355B"
        }
    }

    /**
     * 为当前组找到最佳的开始和结束卷
     */
    private fun findBestEndpointsForGroup(
        group: List<PieceStep>,
        previousEndpoint: PieceStep
    ): Pair<PieceStep, PieceStep> {
        // 评估所有可能的开始卷
        val startCandidates = group.map { piece ->
            piece to calculateTransitionCost(previousEndpoint, piece)
        }.sortedBy { it.second }

        // 选择前几个候选开始卷
        val topStarts = startCandidates.take(3).map { it.first }

        // 为每个候选开始卷找到最佳结束卷
        val endpointPairs = topStarts.flatMap { start ->
            group.filter { it != start }.map { end ->
                Triple(start, end, calculateGroupInternalCost(group, start, end))
            }
        }

        // 选择总成本最低的配对
        return endpointPairs.minByOrNull { it.third }?.let {
            it.first to it.second
        } ?: run {
            // 处理只有一卷的情况
            val onlyPiece = group.first()
            onlyPiece to onlyPiece
        }
    }

    /**
     * 第二阶段：优化组内顺序
     */
    private fun optimizeGroupSequence(
        group: List<PieceStep>,
        startPiece: PieceStep,
        endPiece: PieceStep
    ): List<PieceStep> {
        if (group.size <= 2) return group

        // 分离开始和结束卷
        val remainingPieces = group.filter { it != startPiece && it != endPiece }.toMutableList()

        // 构建路径：开始 -> 中间卷 -> 结束
        val path = mutableListOf(startPiece)
        var current = startPiece

        // 使用最近邻算法构建路径
        while (remainingPieces.isNotEmpty()) {
            val next = findBestNextPiece(current, remainingPieces)
            path.add(next)
            remainingPieces.remove(next)
            current = next
        }

        // 添加结束卷
        path.add(endPiece)

        return path
    }

    /**
     * 找到下一个最佳卷（考虑厚度过渡和属性变化）
     */
    private fun findBestNextPiece(current: PieceStep, candidates: List<PieceStep>): PieceStep {
        return candidates.minByOrNull { calculateTransitionCost(current, it) } ?: candidates.first()
    }

    /**
     * 计算两个卷之间的过渡成本
     */
    private fun calculateTransitionCost(from: PieceStep, to: PieceStep): Double {
        // 检查厚度是否在允许范围内
        val thickness = to.thickness
        val isInPrimaryRange = thickness >= from.minThickForNext && thickness <= from.maxThickForNext
        val isInSecondaryRange = thickness >= from.nextMinThick && thickness <= from.nextMaxThick

        // 基础厚度过渡成本
        val thicknessCost = when {
            isInPrimaryRange -> 0.0 // 在最优范围内
            isInSecondaryRange -> 10.0 // 在次要范围内
            else -> 100.0 + abs(from.thickness - to.thickness) // 超出范围，惩罚
        }

        // 属性变化成本
        val attributeCost = calculateAttributeChangeCost(from, to)

        // 薄卷优先成本（鼓励薄卷排在前面）
        val thinBonus = if (to.thickness < THIN_THRESHOLD) -5.0 else 0.0

        return thicknessCost + attributeCost + thinBonus
    }

    /**
     * 计算属性变化成本
     */
    private fun calculateAttributeChangeCost(from: PieceStep, to: PieceStep): Double {
        var cost = 0.0

        if (from.width != to.width) cost += PENALTY_WEIGHT
        if (from.innerSteelGrade != to.innerSteelGrade) cost += PENALTY_WEIGHT
        if (from.steelGrade != to.steelGrade) cost += PENALTY_WEIGHT

        return cost
    }

    /**
     * 计算组内从开始到结束的总成本估计
     */
    private fun calculateGroupInternalCost(
        group: List<PieceStep>,
        start: PieceStep,
        end: PieceStep
    ): Double {
        if (group.size <= 2) return 0.0

        // 简单估计：计算从开始到结束经过所有卷的平均成本
        val remainingPieces = group.filter { it != start && it != end }
        var totalCost = calculateTransitionCost(start, end) // 直接成本

        // 添加从开始到其他卷的平均成本
        if (remainingPieces.isNotEmpty()) {
            val avgStartCost = remainingPieces.map { calculateTransitionCost(start, it) }.average()
            val avgEndCost = remainingPieces.map { calculateTransitionCost(it, end) }.average()
            totalCost += (avgStartCost + avgEndCost) / 2
        }

        return totalCost
    }

    /**
     * 同钢种在多组时，厚度小的在前面的组中
     */
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

    /**
     * 按照钢种变化分组
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
        result.add(currentGroup)

        return result
    }

    /**
     * 优化方法入口
     */
    fun optimalSequenceForMainPart(mainPartToPlan: MutableList<PieceStep>): MutableList<PieceStep> {
        val groupInnerSteelGradeList = adjustForThinFirst(splitByInnerSteelGradeChange(mainPartToPlan))
        return optimizeThicknessTransition(groupInnerSteelGradeList).toMutableList()
    }
}