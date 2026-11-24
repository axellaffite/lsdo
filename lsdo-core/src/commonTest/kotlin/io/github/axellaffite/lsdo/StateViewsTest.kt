package io.github.axellaffite.lsdo

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StateViewsTest {
    @Test
    fun mapStateValueFollowsTheSourceWithoutAnyCollector() {
        val source = MutableStateFlow(1)
        val view = source.mapState { it * 10 }
        assertEquals(10, view.value)
        source.value = 2
        assertEquals(20, view.value)
        assertEquals(listOf(20), view.replayCache)
    }

    @Test
    fun combineStateValueFollowsBothSources() {
        val a = MutableStateFlow(1)
        val b = MutableStateFlow("x")
        val view = combineState(a, b) { n, s -> "$s$n" }
        assertEquals("x1", view.value)
        a.value = 2
        b.value = "y"
        assertEquals("y2", view.value)
    }

    @Test
    fun collectEmitsDistinctChanges() = runTest {
        val source = MutableStateFlow(1)
        val view = source.mapState { it / 10 } // 1 and 5 map to the same 0
        val seen = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { view.collect { seen += it } }
        source.value = 5
        source.value = 10
        source.value = 15
        source.value = 20
        job.cancel()
        assertEquals(listOf(0, 1, 2), seen)
    }

    @Test
    fun combineStateCollectEmitsDistinctChanges() = runTest {
        val a = MutableStateFlow(1)
        val b = MutableStateFlow(1)
        val view = combineState(a, b) { x, y -> x + y }
        val seen = mutableListOf<Int>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { view.collect { seen += it } }
        a.value = 2 // 3
        b.value = 0 // 2
        a.value = 3 // 3
        b.value = 0 // no change in b
        job.cancel()
        assertEquals(listOf(2, 3, 2, 3), seen)
    }
}
