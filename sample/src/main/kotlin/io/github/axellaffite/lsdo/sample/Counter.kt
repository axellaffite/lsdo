package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.Doc
import io.github.axellaffite.lsdo.LsDoScreen
import io.github.axellaffite.lsdo.lsdoSpec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class CounterState(
    @Doc("The current count") val count: Int = 0,
    @Doc("True while a slow_add is saving") val saving: Boolean = false,
)

@Serializable
sealed interface CounterIntent {
    @Serializable @SerialName("add") @Doc("Add n to the count")
    data class Add(@Doc("How much to add; positive") val n: Int = 1) : CounterIntent

    @Serializable @SerialName("slow_add") @Doc("Add n after a delay, as a save to a server would; the screen is busy meanwhile. Not while saving")
    data class SlowAdd(val n: Int = 1) : CounterIntent

    @Serializable @SerialName("reset") @Doc("Back to zero. Not at zero")
    data object Reset : CounterIntent
}

/** A screen as an app would have it, usually its ViewModel: the UI and LS-DO call the same code. */
class CounterScreen(private val scope: CoroutineScope, private val slow: Duration = 1.seconds) : LsDoScreen<CounterState, CounterIntent> {
    override val lsdo = lsdoSpec<CounterState, CounterIntent>()

    private val _state = MutableStateFlow(CounterState())
    override val state: StateFlow<CounterState> = _state.asStateFlow()

    override suspend fun perform(intent: CounterIntent): String? = when (intent) {
        is CounterIntent.Add -> positive(intent.n) { add(intent.n) }
        is CounterIntent.SlowAdd -> positive(intent.n) { slowAdd(intent.n) }
        CounterIntent.Reset -> null.also { _state.update { it.copy(count = 0) } }
    }

    override fun availableIntents(state: CounterState): Set<String> = buildSet {
        add("add")
        if (!state.saving) add("slow_add")
        if (state.count != 0) add("reset")
    }

    override fun isBusy(state: CounterState): Boolean = state.saving

    fun add(n: Int) = _state.update { it.copy(count = it.count + n) }

    // Returns at once, like a click handler; LS-DO answers once `saving` is false again.
    fun slowAdd(n: Int) {
        _state.update { it.copy(saving = true) }
        scope.launch {
            delay(slow)
            _state.update { it.copy(count = it.count + n, saving = false) }
        }
    }

    private inline fun positive(n: Int, action: () -> Unit): String? =
        if (n <= 0) "Only positive numbers." else null.also { action() }
}
