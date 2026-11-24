package io.github.axellaffite.lsdo

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * A screen an agent can drive: its state [S] (serializable) and its actions [I] (a sealed,
 * serializable type, one subclass per action, named by `@SerialName`). Usually the screen's
 * ViewModel, so an action runs the same code as the UI.
 */
public interface LsDoScreen<S : Any, I : Any> {
    public val lsdo: LsDoSpec<S, I>
    public val state: StateFlow<S>

    /** Runs [intent]; null when it was done, otherwise why it was refused. */
    public suspend fun perform(intent: I): String?

    /** The action names that make sense in [state]; null means all of them. */
    public fun availableIntents(state: S): Set<String>? = null

    /** True while the screen is loading or saving; an action waits for it to be false. */
    public fun isBusy(state: S): Boolean = false
}

public class LsDoSpec<S : Any, I : Any>(public val state: KSerializer<S>, public val intent: KSerializer<I>)

public inline fun <reified S : Any, reified I : Any> lsdoSpec(): LsDoSpec<S, I> = LsDoSpec(serializer<S>(), serializer<I>())
