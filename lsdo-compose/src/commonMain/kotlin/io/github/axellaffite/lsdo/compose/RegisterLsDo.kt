package io.github.axellaffite.lsdo.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import io.github.axellaffite.lsdo.LsDo
import io.github.axellaffite.lsdo.LsDoScreen

/**
 * Makes [screen] drivable under [name] while this composable is in the composition. A no-op when no
 * LS-DO runtime is installed (release builds).
 */
@Composable
public fun RegisterLsDo(name: String, screen: LsDoScreen<*, *>) {
    DisposableEffect(name, screen) {
        val remove = LsDo.register(name, screen)
        onDispose { remove() }
    }
}
