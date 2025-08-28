package com.yongfeng.aps

class PieceStep {
    var pieceStepNr = ""//主键
    var sequence = 0//原始序号
    var width = 0.0//宽度
    var thickness = 0.0//厚度
    var innerSteelGrade = ""//内部钢种
    var steelGrade = ""//钢种
    var minThickForNext = 0.0//小下限
    var maxThickForNext = 0.0//小上限
    var nextMaxThick = 0.0//大上限
    var nextMinThick = 0.0//大下限
}