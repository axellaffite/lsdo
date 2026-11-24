package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.runtime.LsDoJson
import io.github.axellaffite.lsdo.runtime.LsDoRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class CounterScreenTest {
    @Test
    fun aSlowAddAnswersOnceTheSaveIsDone() = runBlocking {
        val counter = CounterScreen(CoroutineScope(SupervisorJob() + Dispatchers.Default), slow = 100.milliseconds)
        val registry = LsDoRegistry(settle = 10.milliseconds)
        registry.register("counter", counter)

        val outcome = registry.perform("counter", LsDoJson.parseToJsonElement("""{"type":"slow_add","n":2}"""))

        assertTrue(outcome.ok, outcome.message)
        assertEquals(CounterState(count = 2, saving = false), counter.state.value)
    }
}
