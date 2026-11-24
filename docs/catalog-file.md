# A committed catalog file

A committed `lsdo-catalog.json` lets an agent plan a route through the app without a device. A JVM
test writes or compares it, so it cannot go stale ([the sample](../sample) does exactly this):

```kotlin
// src/jvmTest/kotlin/.../LsDoCatalogFileTest.kt
class LsDoCatalogFileTest {
    private val json = Json { prettyPrint = true }

    @Test
    fun theCommittedCatalogMatchesTheCode() {
        val file = File(requireNotNull(System.getProperty("lsdo.catalog")))
        val generated = json.encodeToString(JsonElement.serializer(), catalogFull(AppLsDoScreens)) + "\n"
        if (System.getProperty("lsdo.catalog.write") == "true") return file.writeText(generated)
        val committed = if (file.exists()) file.readText() else ""
        assertEquals(generated, committed, "lsdo-catalog.json is stale: run ./gradlew lsdoCatalog")
    }
}
```

```kotlin
// build.gradle.kts of that module (lsdo-runtime as a jvmTest dependency)
val lsdoCatalogFile = rootProject.file("lsdo-catalog.json")
val lsdoCatalog = tasks.register<Test>("lsdoCatalog") {
    val jvmTest = tasks.named<Test>("jvmTest").get()
    testClassesDirs = jvmTest.testClassesDirs
    classpath = jvmTest.classpath
    filter { includeTestsMatching("*LsDoCatalogFileTest") }
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    systemProperty("lsdo.catalog.write", "true")
    outputs.file(lsdoCatalogFile)
    outputs.upToDateWhen { false }
}
tasks.named<Test>("jvmTest") {
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    inputs.files(lsdoCatalogFile).withPropertyName("lsdoCatalog")
    mustRunAfter(lsdoCatalog)
}
```

