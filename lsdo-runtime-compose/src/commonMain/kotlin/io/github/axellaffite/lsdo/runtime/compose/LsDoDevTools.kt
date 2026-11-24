package io.github.axellaffite.lsdo.runtime.compose

import io.github.axellaffite.lsdo.LsDo
import io.github.axellaffite.lsdo.LsDoEntry
import io.github.axellaffite.lsdo.runtime.LSDO_PORT
import io.github.axellaffite.lsdo.runtime.LsDoRegistry
import io.github.axellaffite.lsdo.runtime.LsDoServer
import io.github.axellaffite.lsdo.runtime.UiNode
import io.github.axellaffite.lsdo.runtime.startLsDoServer
import kotlinx.coroutines.Dispatchers

/**
 * One call that makes a debug app drivable: installs the registry (actions run on the main thread),
 * the platform's UI capture and the server on 127.0.0.1:[port]. Call it once, from debug code only.
 *
 * Android: call it in `Application.onCreate`, before any Compose view exists, or `ui` misses the
 * roots created earlier. iOS: call it before the first Compose view controller is made.
 */
public object LsDoDevTools {
    // Started once per process; a second call returns the running server instead of a port clash.
    private var server: LsDoServer? = null

    /** The running server, or null when it could not start (the reason is logged). */
    public fun start(catalog: List<LsDoEntry> = emptyList(), port: Int = LSDO_PORT): LsDoServer? {
        server?.let { return it }
        val registry = LsDoRegistry(context = Dispatchers.Main.immediate, capture = platformCapture(), catalog = catalog)
        LsDo.install(registry)
        return runCatching { startLsDoServer(registry, port) }
            .onSuccess {
                server = it
                log("LS-DO listening on 127.0.0.1:$port", null)
            }
            .onFailure { log("LS-DO server did not start", it) }
            .getOrNull()
    }
}

/** Installs whatever the platform needs to see its UI, and returns the reader. */
internal expect fun platformCapture(): suspend () -> List<UiNode>

internal expect fun log(message: String, error: Throwable?)
