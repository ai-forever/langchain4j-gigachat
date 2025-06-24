plugins {
    id("java")
    `java-library`
    id("io.freefair.lombok") version "8.12.2"
    id("langchain4j-gigachat.publish")
}

repositories {
    mavenCentral()
}

dependencies {
    api("dev.langchain4j:langchain4j:1.1.0")
    api("chat.giga:gigachat-java:0.1.10")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    testImplementation("io.github.dvgaba:easy-random-core:7.1.0")
}

tasks.test {
    useJUnitPlatform()
}
