plugins {
    id("java")
    `java-library`
    id("io.freefair.lombok") version "8.12.2"

}

group = "chat.giga.langchain4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("dev.langchain4j:langchain4j:1.0.0-beta2")
    api("chat.giga:gigachat-java:0.1.2")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}