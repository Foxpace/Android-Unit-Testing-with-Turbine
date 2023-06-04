package com.tomasrepcik.turbineexample.repo

interface HeavyComputationTemplate {

    suspend fun doComputation(): String

}