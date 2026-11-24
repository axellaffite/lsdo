package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.Doc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// A screen that is never shown in this sample: it is in the catalog only, as a screen the app has
// but that is not on show now would be.

@Serializable
data class SettingsState(@Doc("Dark theme on") val dark: Boolean = false)

@Serializable
sealed interface SettingsIntent {
    @Serializable @SerialName("set_dark") @Doc("Turn the dark theme on or off")
    data class SetDark(val on: Boolean) : SettingsIntent
}
