plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}

dependencies {
    implementation(project(":lsdo-runtime"))
    // Ktor logs through SLF4J; the sample has nothing to say there.
    runtimeOnly(libs.slf4j.nop)
    testImplementation(libs.kotlin.test)
}

application {
    mainClass.set("io.github.axellaffite.lsdo.sample.MainKt")
}

// The catalog drift check: test compares sample/lsdo-catalog.json with the code, lsdoCatalog rewrites it.
val lsdoCatalogFile = layout.projectDirectory.file("lsdo-catalog.json").asFile
val lsdoCatalog = tasks.register<Test>("lsdoCatalog") {
    group = "lsdo"
    description = "Writes sample/lsdo-catalog.json from the sample's LS-DO screens."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("io.github.axellaffite.lsdo.sample.CatalogFileTest") }
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    systemProperty("lsdo.catalog.write", "true")
    outputs.file(lsdoCatalogFile)
    outputs.upToDateWhen { false }
}
tasks.test {
    systemProperty("lsdo.catalog", lsdoCatalogFile.absolutePath)
    inputs.files(lsdoCatalogFile).withPropertyName("lsdoCatalog")
    mustRunAfter(lsdoCatalog)
}
