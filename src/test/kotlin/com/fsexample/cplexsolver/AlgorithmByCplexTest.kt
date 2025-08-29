package com.fsexample.cplexsolver

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AlgorithmByCplexTest {
    var algorithmByCplex: AlgorithmByCplex? = null

    @BeforeEach
    fun setup() {
        algorithmByCplex = AlgorithmByCplex()
    }

    @Test
    fun test() {
        algorithmByCplex?.testCplexDependency()
    }
}