plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":langchain4j-gigachat"))
    implementation("dev.langchain4j:langchain4j-mcp:1.15.1-beta25")
    implementation("ch.qos.logback:logback-classic:1.5.34")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
