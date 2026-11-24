package io.github.axellaffite.lsdo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class LsDoTest {
    @Serializable
    data class Empty(val x: Int = 0)

    private val screen = object : LsDoScreen<Empty, Empty> {
        override val lsdo = lsdoSpec<Empty, Empty>()
        override val state: StateFlow<Empty> = MutableStateFlow(Empty())
        override suspend fun perform(intent: Empty): String? = null
    }

    @Test
    fun registeringGoesToTheInstalledSinkAndItsRemoverIsReturned() {
        val registered = mutableListOf<String>()
        LsDo.install { name, _ ->
            registered += name
            ({ registered -= name })
        }

        val remove = LsDo.register("home", screen)
        assertEquals(listOf("home"), registered)
        remove()
        assertEquals(emptyList(), registered)
    }
}
