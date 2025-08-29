package com.fsexample.cplexsolver

import ilog.concert.IloException
import ilog.concert.IloLinearNumExpr
import ilog.cplex.IloCplex

class AlgorithmByCplex {

    /**
     * 优化demo1的写法
     * max z= 4x_1 + 3x_2
     * s.t.
     * 2x_1 + x_2 <= 10
     * x_1 + x_2 <= 8
     * x_2 <= 7
     * x_1,x_2 >= 0
     */
    fun testCplexDependency() {
        var cplex: IloCplex? = null
        try {
            cplex = IloCplex()
            // 使用Double.POSITIVE_INFINITY而不是Double.MAX_VALUE
            val x1 = cplex.numVar(0.0, Double.POSITIVE_INFINITY, "x1")
            val x2 = cplex.numVar(0.0, 7.0, "x2")

            // 使用表达式并命名约束
            val expr1: IloLinearNumExpr = cplex.linearNumExpr().apply {
                addTerm(2.0, x1)
                addTerm(1.0, x2)
            }
            cplex.addLe(expr1, 10.0, "constraint1")

            val expr2: IloLinearNumExpr = cplex.linearNumExpr().apply {
                addTerm(1.0, x1)
                addTerm(1.0, x2)
            }
            cplex.addLe(expr2, 8.0, "constraint2")

            val objective: IloLinearNumExpr = cplex.linearNumExpr().apply {
                addTerm(4.0, x1)
                addTerm(3.0, x2)
            }
            cplex.addMaximize(objective)

            // 求解
            if (cplex.solve()) {
                println("Solution status = " + cplex.status)
                println("Solution value = " + cplex.objValue)
                println("x1 = " + cplex.getValue(x1))
                println("x2 = " + cplex.getValue(x2))
            }
        } catch (e: IloException) {
            System.err.println("Concert exception caught: $e")
        } finally {
            cplex?.end()
        }
    }
}