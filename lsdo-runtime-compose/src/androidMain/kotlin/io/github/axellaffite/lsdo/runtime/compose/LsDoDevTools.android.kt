package io.github.axellaffite.lsdo.runtime.compose

import android.util.Log
import io.github.axellaffite.lsdo.runtime.UiNode

internal actual fun platformCapture(): suspend () -> List<UiNode> {
    ComposeUiCapture.install()
    return ComposeUiCapture::capture
}

internal actual fun log(message: String, error: Throwable?) {
    if (error == null) Log.i("LsDo", message) else Log.w("LsDo", message, error)
}
