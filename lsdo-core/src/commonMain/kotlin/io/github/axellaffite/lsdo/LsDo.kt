package io.github.axellaffite.lsdo

import kotlin.concurrent.Volatile

/**
 * Where screens announce themselves. Release builds never install a sink, so [register] does nothing
 * there and the app needs no LS-DO runtime to compile.
 */
public object LsDo {
    public fun interface Sink {
        /** Adds [screen] under [name]; the returned function removes it. */
        public fun register(name: String, screen: LsDoScreen<*, *>): () -> Unit
    }

    @Volatile
    private var sink: Sink? = null

    public val installed: Boolean get() = sink != null

    public fun install(sink: Sink) {
        this.sink = sink
    }

    /** Registers [screen] with the installed sink, if any; call the result when the screen leaves. */
    public fun register(name: String, screen: LsDoScreen<*, *>): () -> Unit = sink?.register(name, screen) ?: {}
}
