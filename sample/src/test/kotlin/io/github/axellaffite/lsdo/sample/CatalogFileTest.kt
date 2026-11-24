package io.github.axellaffite.lsdo.sample

import io.github.axellaffite.lsdo.runtime.catalogFull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The committed catalog (sample/lsdo-catalog.json) matches the code; `./gradlew :sample:lsdoCatalog` rewrites it. */
class CatalogFileTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun theCommittedCatalogMatchesTheCode() {
        val file = File(requireNotNull(System.getProperty("lsdo.catalog")) { "lsdo.catalog is set by the build" })
        val generated = json.encodeToString(JsonElement.serializer(), catalogFull(SampleCatalog)) + "\n"
        if (System.getProperty("lsdo.catalog.write") == "true") {
            file.writeText(generated)
            return
        }
        val committed = if (file.exists()) file.readText() else ""
        assertEquals(generated, committed, "sample/lsdo-catalog.json is stale: run ./gradlew :sample:lsdoCatalog")
    }
}
