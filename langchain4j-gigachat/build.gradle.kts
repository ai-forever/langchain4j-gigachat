plugins {
    id("java")
    `java-library`
    id("io.freefair.lombok") version "8.12.2"
    id("langchain4j-gigachat.publish")
}

group = "chat.giga.langchain4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("dev.langchain4j:langchain4j:1.0.0-beta2")
    api("chat.giga:gigachat-java:0.1.5")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    testImplementation("io.github.dvgaba:easy-random-core:7.1.0")
}

tasks.test {
    useJUnitPlatform()
}
