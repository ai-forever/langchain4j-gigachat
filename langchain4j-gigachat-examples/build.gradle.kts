plugins {
    id("java")
}

group = "chat.giga.langchain4j"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":langchain4j-gigachat"))
    implementation("ch.qos.logback:logback-classic:1.5.8")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}