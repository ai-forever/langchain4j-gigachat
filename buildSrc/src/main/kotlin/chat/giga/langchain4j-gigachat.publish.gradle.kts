import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.vanniktech.maven.publish")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

extra["signingInMemoryKey"] = System.getenv("GPG_SIGNING_KEY")
extra["signingInMemoryKeyId"] = System.getenv("GPG_SIGNING_KEY_ID")
extra["signingInMemoryKeyPassword"] = System.getenv("GPG_SIGNING_PASSWORD")

configure<MavenPublishBaseExtension> {
    signAllPublications()
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    coordinates(project.group.toString(), project.name, project.version.toString())
    pom {
        name.set("LangChain4j GigaChat")
        url.set("https://github.com/ai-forever/langchain4j-gigachat")
        description.set("langchain4j-gigachat.")
        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("http://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("isergeymd, dmbocharova")
                name.set("Sergey Safonov, Darya Bocharova")
                email.set("SSafonov@sberbank.ru, dmbocharova@sberbank.ru")
            }
        }
        scm {
            connection.set("scm:https://github.com/ai-forever/langchain4j-gigachat.git")
            developerConnection.set("scm:git@github.com:ai-forever/langchain4j-gigachat.git")
            url.set("https://github.com/ai-forever/langchain4j-gigachat")
        }
    }
}