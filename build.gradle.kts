plugins {
    id("io.micronaut.application") version "5.0.2"
    id("com.gradleup.shadow") version "9.4.1"
}

version = "1.0.0"
group = "uk.police.k9"

val mapstructVersion = "1.6.3"

repositories {
    mavenCentral()
}

dependencies {
    // MapStruct must come before the Micronaut processors so the mappers it generates are
    // visible to Micronaut's bean definition processor in the same compilation round.
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    annotationProcessor("io.micronaut.data:micronaut-data-processor")
    annotationProcessor("io.micronaut:micronaut-http-validation")
    annotationProcessor("io.micronaut.openapi:micronaut-openapi")
    annotationProcessor("io.micronaut.serde:micronaut-serde-processor")
    annotationProcessor("io.micronaut.validation:micronaut-validation-processor")

    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.validation:jakarta.validation-api")
    // Micronaut Data's criteria API is built on the JPA criteria types, so the API jar is
    // needed even though no JPA provider is used.
    implementation("jakarta.persistence:jakarta.persistence-api")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")

    compileOnly("io.micronaut:micronaut-http-client")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")

    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.h2database:h2")

    testAnnotationProcessor("io.micronaut:micronaut-inject-java")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass = "uk.police.k9.dogs.Application"
}

java {
    // Micronaut 5's Gradle plugin requires a JDK 25 toolchain.
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("uk.police.k9.dogs.*")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(
        listOf(
            "-Amapstruct.defaultComponentModel=jsr330",
            "-Amapstruct.unmappedTargetPolicy=ERROR",
            "-Amapstruct.suppressGeneratorTimestamp=true",
        )
    )

    // micronaut-openapi reads openapi.properties to decide which documentation views to write
    // into META-INF/swagger/views, but Gradle cannot see that the file is consulted: it is read
    // by the processor, not passed on the compiler command line. Without declaring it, enabling a
    // view leaves compileJava up to date - or served from the build cache, which a `clean` does
    // not help with - and the view silently never appears. Declaring it as an input makes a
    // toggle invalidate the task, so the assets are regenerated on the next ordinary build.
    inputs.file(rootProject.layout.projectDirectory.file("openapi.properties"))
        .withPropertyName("openapiProperties")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}

// Starts with the sample register loaded, so there is something to look at immediately.
tasks.named<JavaExec>("run") {
    environment("MICRONAUT_ENVIRONMENTS", "dev")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// https://docs.gradle.org/current/userguide/upgrading_major_version_9.html#test_task_fails_when_no_tests_are_discovered
tasks.withType<AbstractTestTask>().configureEach {
    failOnNoDiscoveredTests = false
}
