plugins {
    `java-library`
    `application`
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.google.guava:guava")
    implementation("com.markelliot.barista:barista")
    implementation("com.markelliot.barista.tracing:barista-tracing")
    implementation("com.markelliot.result:result")

    compileOnly("com.markelliot.barista:barista-annotations")
    annotationProcessor("com.markelliot.barista:barista-processor")

    compileOnly("org.immutables:value::annotations")
    annotationProcessor("org.immutables:value")

    testImplementation(platform("org.junit:junit-bom"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
