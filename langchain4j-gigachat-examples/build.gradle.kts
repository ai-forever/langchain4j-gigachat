plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":langchain4j-gigachat"))
    implementation("dev.langchain4j:langchain4j-mcp:1.10.0-beta18")
    implementation("ch.qos.logback:logback-classic:1.5.25")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
