package io.github.axellaffite.lsdo.runtime.compose

import kotlin.test.Test

class LogTest {
    @Test
    fun logsPlainMessage() {
        log("LS-DO listening on 127.0.0.1:8080", null)
    }

    @Test
    fun logsMessageWithFormatSpecifiersAndError() {
        log("100% %@ %d %s", IllegalStateException("bad %@ value"))
    }
}
