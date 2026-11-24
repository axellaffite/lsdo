package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.LsDoEntry
import io.github.axellaffite.lsdo.lsdoSpec

/** Every screen of the app, shown or not, with how to reach it. */
val SampleCatalog: List<LsDoEntry> = listOf(
    LsDoEntry("counter", "A counter. Always shown.", lsdoSpec<CounterState, CounterIntent>()),
    LsDoEntry("settings", "The app's settings. Never shown in this sample: it is here to show the catalog.", lsdoSpec<SettingsState, SettingsIntent>()),
)
