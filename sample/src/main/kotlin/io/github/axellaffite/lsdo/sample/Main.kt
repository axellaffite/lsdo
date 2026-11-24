package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.LsDo
import io.github.axellaffite.lsdo.runtime.LSDO_PORT
import io.github.axellaffite.lsdo.runtime.LsDoRegistry
import io.github.axellaffite.lsdo.runtime.UiNode
import io.github.axellaffite.lsdo.runtime.startLsDoServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking

fun main(): Unit = runBlocking {
    val counter = CounterScreen(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    val registry = LsDoRegistry(capture = { counterUi(counter.state.value) }, catalog = SampleCatalog)
    LsDo.install(registry)
    // An app does this from the screen's composable (RegisterLsDo); the sample has one screen, always shown.
    LsDo.register("counter", counter)
    val port = System.getenv("LSDO_PORT")?.toIntOrNull() ?: LSDO_PORT
    startLsDoServer(registry, port)
    println("LS-DO sample listening on http://127.0.0.1:$port (Ctrl-C to stop)")
    awaitCancellation()
}

/**
 * A console program renders nothing, so this stands in for the platform capture an app gets from
 * lsdo-runtime-compose: the window the counter would show, built from its state.
 */
fun counterUi(state: CounterState): List<UiNode> = listOf(
    UiNode(
        role = "window",
        children = listOfNotNull(
            UiNode(role = "heading", text = "Counter"),
            UiNode(role = "text", text = "Count", value = state.count.toString()),
            UiNode(role = "button", text = "Add", actions = listOf("click")),
            UiNode(role = "button", text = "Reset", flags = if (state.count == 0) listOf("disabled") else emptyList(), actions = listOf("click")),
            if (state.saving) UiNode(role = "progress", text = "Saving") else null,
        ),
    ),
)
