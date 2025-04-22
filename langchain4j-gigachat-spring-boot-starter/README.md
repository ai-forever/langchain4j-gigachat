# Примеры работы с LangChain4j GigaChat в Spring Boot Framework

## Установка

Чтобы установить библиотеку, подключите ее в зависимости.

### Gradle

```kotlin
implementation("chat.giga:langchain4j-gigachat-spring-boot-starter:0.1.4")
```

### Maven

```xml

<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>langchain4j-gigachat-spring-boot-starter</artifactId>
    <version>0.1.4</version>
</dependency>
```

Вы можете сконфигурировать модель в application.properties файле таким способом:

```properties
langchain4j.gigachat.chat-model.model-name=GigaChat-Max
langchain4j.gigachat.chat-model.auth.type=OAUH
langchain4j.gigachat.chat-model.auth.auth-key=your-key
langchain4j.gigachat.chat-model.auth.scope=GIGACHAT_API_PERS
langchain4j.gigachat.chat-model.log-requests=true
```

В таком случае сущность GigaChatChatModel будет автоматически создана и вы сможете заинжектить ее, где это необходимо

```java

@RestController
public class ChatController {

    ChatLanguageModel chatLanguageModel;

    public ChatController(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @GetMapping("/chat")
    public String model(@RequestParam(value = "message", defaultValue = "Hello") String message) {
        return chatLanguageModel.chat(message);
    }
}
```

Поддержаны все основные модели, названия property:

- chat-model
- streaming-chat-model
- embedding-model
- image-model

Еще документация по Spring Boot [тут](https://docs.langchain4j.dev/tutorials/spring-boot-integration/)

