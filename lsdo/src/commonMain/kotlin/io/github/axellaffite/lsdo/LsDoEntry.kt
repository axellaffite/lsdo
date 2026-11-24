package io.github.axellaffite.lsdo

/**
 * One screen of the catalog: every screen the app has, shown or not, so an agent can plan a route
 * before it is on screen. [description] says what the screen is and how to reach it.
 */
public class LsDoEntry(public val name: String, public val description: String, public val spec: LsDoSpec<*, *>)
