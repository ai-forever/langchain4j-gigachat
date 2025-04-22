# GigaChat для LangChain4j

GigaChat для LangChain4j — это Java-библиотека, которая интегрирует модели GigaChat c проектом [LangChain4j](https://docs.langchain4j.dev/).
Библиотека предназначена для упрощения разработки LLM-приложений на Java.
Для обмена сообщения с моделями она использует [GigaChat Java SDK](https://github.com/ai-forever/gigachat-java/).
Библиотека входит в состав [GigaChain](https://github.com/ai-forever/gigachain/) — набора решений для разработки LLM-приложений с помощью моделей GigaChat.

## О проекте LangChain4j

Проект LangChain4j существует с 2023 года и основывается на концептах, предложенных популярными фреймворками для разработки LLM-приложений, среди которых LangChain, Haystack, LlamaIndex.
Он дает доступ к единообразным API, широкому набору программных инструментов и [библиотеке примеров](https://github.com/langchain4j/langchain4j-examples).

> [!TIP]
> Примеры работы с моделями GigaChat с помощью библиотеки `langchain4j-gigachat` — в папке [langchain4j-gigachat-examples](langchain4j-gigachat-examples/README.md).

## Требования

Для работы `langchain4j-gigachat` используйте Java версии 17 или выше.

## Установка

Чтобы установить библиотеку, подключите ее в зависимости.

### Gradle

```kotlin
implementation("chat.giga:langchain4j-gigachat:0.1.2")
```

### Maven

```xml

<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>langchain4j-gigachat</artifactId>
    <version>0.1.2</version>
</dependency>
```

## Примеры

Ниже приводится несколько базовых примеров работы с `langchain4j-gigachat`.
Полный список примеров — в папке [langchain4j-gigachat-examples](langchain4j-gigachat-examples/README.md).

### Инициализация модели для генерации

Авторизация запросов к GigaChat API выполняется с помощью ключа авторизации.
Вы также можете использовать другие [способы аутентификации](https://github.com/ai-forever/gigachat-java/?tab=readme-ov-file#%D1%81%D0%BF%D0%BE%D1%81%D0%BE%D0%B1%D1%8B-%D0%B0%D1%83%D1%82%D0%B5%D0%BD%D1%82%D0%B8%D1%84%D0%B8%D0%BA%D0%B0%D1%86%D0%B8%D0%B8), которые поддерживает GigaChat Java SDK.

```java
GigaChatChatModel model = GigaChatChatModel.builder()
        .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .build())
        .authClient(AuthClient.builder()
                .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                        .scope(Scope.GIGACHAT_API_PERS)
                        .authKey("<ключ_авторизации>")
                        .build())
                .build())
        .logRequests(true)
        .logResponses(true)
        .build();
```

### Инициализация потоковой генерации токенов

```java
GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
        .authClient(AuthClient.builder()
                .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                        .scope(Scope.GIGACHAT_API_PERS)
                        .authKey("<ключ_авторизации>")
                        .build())
                .build())
        .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                .temperature(1.0)
                .modelName(ModelName.GIGA_CHAT_PRO)
                .responseFormat(JsonSchema.builder().build())
                .build())
        .logRequests(true)
        .logResponses(true)
        .build();
```

### Инициализация модели для создания эмбеддингов

```java
GigaChatEmbeddingModel model = GigaChatEmbeddingModel.builder()
        .authClient(AuthClient.builder()
                .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                        .scope(Scope.GIGACHAT_API_PERS)
                        .authKey("<ключ_авторизации>")
                        .build())
                .build())
        .logRequests(true)
        .logResponses(true)
        .build();
```

## Полезные материалы

* [Официальная документация LangChain4j (англ.)](https://docs.langchain4j.dev/intro);
* Серия обучающих статей (англ.):
    * [Part 1: Getting Started with Generative AI using Java, LangChain4j, OpenAI and Ollama](https://www.sivalabs.in/getting-started-with-generative-ai-using-java-langchain4j-openai-ollama/)
    * [Part 2: Generative AI Conversations using LangChain4j ChatMemory](https://www.sivalabs.in/generative-ai-conversations-using-langchain4j-chat-memory/)
    * [Part 3: LangChain4j AiServices Tutorial](https://www.sivalabs.in/langchain4j-ai-services-tutorial/)
    * [Part 4: LangChain4j Retrieval-Augmented Generation (RAG) Tutorial](https://www.sivalabs.in/langchain4j-retrieval-augmented-generation-tutorial/)
* Книга [Understanding LangChain4j](https://agoncal.teachable.com/p/ebook-understanding-langchain4j) (англ.)
