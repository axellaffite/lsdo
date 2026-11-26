package io.github.axellaffite.lsdo.runtime.compose

import io.github.axellaffite.lsdo.runtime.UiNode
import platform.Foundation.NSLog

internal actual fun platformCapture(): suspend () -> List<UiNode> = UiKitCapture::capture

internal actual fun log(message: String, error: Throwable?) {
    val text = if (error == null) message else "$message: $error"
    // A Kotlin String passed as an NSLog vararg is not bridged to NSString, so "%@" would read
    // a bad pointer and crash. Pass the text as the format itself, with its "%" escaped.
    NSLog(text.replace("%", "%%"))
}
