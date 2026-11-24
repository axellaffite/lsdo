package io.github.axellaffite.lsdo.runtime.compose

import io.github.axellaffite.lsdo.runtime.UiNode
import platform.Foundation.NSLog

internal actual fun platformCapture(): suspend () -> List<UiNode> = UiKitCapture::capture

internal actual fun log(message: String, error: Throwable?) {
    NSLog("%@", if (error == null) message else "$message: $error")
}
