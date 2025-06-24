package chat.giga.langchain4j.spring;

import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatEmbeddingModel;
import chat.giga.langchain4j.GigaChatImageModel;
import chat.giga.langchain4j.GigaChatStreamingChatModel;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static chat.giga.langchain4j.spring.AuthType.OAUH;
import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigIT {

    ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AutoConfig.class));

    @Test
    void should_provide_chat_model() {
        contextRunner
                .withPropertyValues(
                        "langchain4j.gigachat.chat-model.model-name=" + ModelName.GIGA_CHAT_MAX,
                        "langchain4j.gigachat.chat-model.auth.type=" + OAUH,
                        "langchain4j.gigachat.chat-model.auth.auth-key=" + Optional.ofNullable(
                                System.getenv("AUTH_KEY")).orElse("testkey"),
                        "langchain4j.gigachat.chat-model.auth.scope=" + Scope.GIGACHAT_API_PERS
                )
                .run(context -> {
                    ChatModel chatLanguageModel = context.getBean(ChatModel.class);
                    assertThat(chatLanguageModel).isInstanceOf(GigaChatChatModel.class);
                });
    }

    @Test
    void should_provide_streaming_chat_model() {
        contextRunner
                .withPropertyValues(
                        "langchain4j.gigachat.streaming-chat-model.model-name=" + ModelName.GIGA_CHAT_MAX,
                        "langchain4j.gigachat.streaming-chat-model.auth.type=" + OAUH,
                        "langchain4j.gigachat.streaming-chat-model.auth.auth-key=" + Optional.ofNullable(
                                System.getenv("AUTH_KEY")).orElse("testkey"),
                        "langchain4j.gigachat.streaming-chat-model.auth.scope=" + Scope.GIGACHAT_API_PERS
                )
                .run(context -> {
                    StreamingChatModel chatLanguageModel = context.getBean(StreamingChatModel.class);
                    assertThat(chatLanguageModel).isInstanceOf(GigaChatStreamingChatModel.class);
                });
    }

    @Test
    void should_provide_embedding_chat_model() {
        contextRunner
                .withPropertyValues(
                        "langchain4j.gigachat.embedding-model.model-name=" + ModelName.GIGA_CHAT_MAX,
                        "langchain4j.gigachat.embedding-model.auth.type=" + OAUH,
                        "langchain4j.gigachat.embedding-model.auth.auth-key=" + Optional.ofNullable(
                                System.getenv("AUTH_KEY")).orElse("testkey"),
                        "langchain4j.gigachat.embedding-model.auth.scope=" + Scope.GIGACHAT_API_PERS
                )
                .run(context -> {
                    DimensionAwareEmbeddingModel chatLanguageModel = context.getBean(
                            DimensionAwareEmbeddingModel.class);
                    assertThat(chatLanguageModel).isInstanceOf(GigaChatEmbeddingModel.class);
                });
    }

    @Test
    void should_provide_image_chat_model() {
        contextRunner
                .withPropertyValues(
                        "langchain4j.gigachat.image-model.model-name=" + ModelName.GIGA_CHAT_MAX,
                        "langchain4j.gigachat.image-model.auth.type=" + OAUH,
                        "langchain4j.gigachat.image-model.auth.auth-key=" + Optional.ofNullable(
                                System.getenv("AUTH_KEY")).orElse("testkey"),
                        "langchain4j.gigachat.image-model.auth.scope=" + Scope.GIGACHAT_API_PERS
                )
                .run(context -> {
                    ImageModel chatLanguageModel = context.getBean(ImageModel.class);
                    assertThat(chatLanguageModel).isInstanceOf(GigaChatImageModel.class);
                });
    }
}
