# LangChain4j GigaChat

langchain4j-gigachat это имплементация [GigaChat](https://developers.sber.ru/docs/ru/gigachat/overview) LLM
для [Langchain4j](https://github.com/langchain4j/langchain4j). Внутри
использует [GigaChat JAVA SDK](https://github.com/ai-forever/gigachat-java/)

Библиотека управляет авторизацией запросов и предоставляет все необходимые методы для работы с API.

> [!TIP]
> Больше примеров работы с библиотекой — в
> папке [langchain4j-gigachat-examples](langchain4j-gigachat-examples/README.md).

## Требования

Для работы библиотеки установите Java версии 17 или выше.

## Установка

Чтобы установить библиотеку, подключите ее в зависимости.

### Gradle

```kotlin
implementation("chat.giga:langchain4j-gigachat:0.1.1")
```

### Maven

```xml

<dependency>
    <groupId>chat.giga</groupId>
    <artifactId>langchain4j-gigachat</artifactId>
    <version>0.1.1</version>
</dependency>
```
