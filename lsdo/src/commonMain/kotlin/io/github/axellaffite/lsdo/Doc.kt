package io.github.axellaffite.lsdo

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialInfo

/**
 * A description for an action (on an intent subclass) or a field (on a property). It is kept in the
 * serial descriptor, so the runtime can put it in the JSON schemas an agent reads.
 */
@OptIn(ExperimentalSerializationApi::class)
@SerialInfo
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
public annotation class Doc(val text: String)
