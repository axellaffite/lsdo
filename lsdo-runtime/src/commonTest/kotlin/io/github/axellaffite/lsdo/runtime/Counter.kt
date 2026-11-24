package io.github.axellaffite.lsdo.runtime

import io.github.axellaffite.lsdo.Doc
import io.github.axellaffite.lsdo.LsDoScreen
import io.github.axellaffite.lsdo.lsdoSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CounterState(
    @Doc("The current count") val count: Int = 0,
    val loading: Boolean = false,
)

@Serializable
sealed interface CounterIntent {
    @Serializable @SerialName("add") @Doc("Adds n to the count")
    data class Add(val n: Int, val note: String? = null) : CounterIntent

    @Serializable @SerialName("reset")
    data object Reset : CounterIntent

    @Serializable @SerialName("fail")
    data object Fail : CounterIntent
}

/** A screen for the tests: "reset" only once the count moved, "fail" throws. */
/** [quietWhileLoading]: offers no action while loading, as a screen still fetching its data might. */
class Counter(private val quietWhileLoading: Boolean = false) : LsDoScreen<CounterState, CounterIntent> {
    override val lsdo = lsdoSpec<CounterState, CounterIntent>()
    private val _state = MutableStateFlow(CounterState())
    override val state: StateFlow<CounterState> = _state

    override suspend fun perform(intent: CounterIntent): String? = when (intent) {
        is CounterIntent.Add -> if (intent.n < 0) "Only positive numbers." else null.also { _state.update { it.copy(count = it.count + intent.n) } }
        CounterIntent.Reset -> null.also { _state.update { it.copy(count = 0) } }
        CounterIntent.Fail -> error("boom")
    }

    override fun availableIntents(state: CounterState): Set<String> = when {
        quietWhileLoading && state.loading -> emptySet()
        state.count == 0 -> setOf("add", "fail")
        else -> setOf("add", "reset", "fail")
    }

    override fun isBusy(state: CounterState): Boolean = state.loading

    fun setLoading(loading: Boolean) = _state.update { it.copy(loading = loading) }
}
