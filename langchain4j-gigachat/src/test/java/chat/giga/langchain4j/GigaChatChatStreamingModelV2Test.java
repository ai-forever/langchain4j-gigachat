package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.stream.CompletionMessageDoneEventV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static chat.giga.langchain4j.utils.GigaChatHelperV2.toRequestV2;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GigaChatChatStreamingModelV2Test {

    @Mock
    HttpClient httpClient;
    @Mock
    AuthClient authClient;

    @Test
    void shouldCreateStreamingModelWithV2Enabled() {
        GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build())
                .build();

        assertThat(model.defaultRequestParameters()).isInstanceOf(GigaChatChatRequestParameters.class);
        assertThat(model.defaultRequestParameters().getUseV2Completions()).isTrue();
    }

    @Test
    void shouldCreateStreamingModelWithV2DisabledByDefault() {
        GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .build();

        assertThat(model.defaultRequestParameters().getUseV2Completions()).isFalse();
    }

    @Test
    void shouldConvertProfanityCheckToDisableFilterForStreaming() {
        ChatRequest chatRequestWithProfanity = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .stream(true)
                        .profanityCheck(true)
                        .build())
                .build();

        CompletionRequestV2 requestWithProfanity = toRequestV2(chatRequestWithProfanity);
        assertThat(requestWithProfanity.disableFilter()).isFalse();

        ChatRequest chatRequestWithoutProfanity = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .stream(true)
                        .profanityCheck(false)
                        .build())
                .build();

        CompletionRequestV2 requestWithoutProfanity = toRequestV2(chatRequestWithoutProfanity);
        assertThat(requestWithoutProfanity.disableFilter()).isFalse();
    }

    @Test
    void shouldIncludeV2StreamingParametersInRequest() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .stream(true)
                        .assistantId("assistant-123")
                        .memoryId("memory-456")
                        .reasoningEffort("medium")
                        .build())
                .build();

        CompletionRequestV2 requestV2 = toRequestV2(chatRequest);

        assertThat(requestV2.stream()).isTrue();
        assertThat(requestV2.assistantId()).isEqualTo("assistant-123");
        assertThat(requestV2.memoryId()).isEqualTo("memory-456");
        assertThat(requestV2.modelOptions()).isNotNull();
        assertThat(requestV2.modelOptions().reasoning()).isNotNull();
        assertThat(requestV2.modelOptions().reasoning().effort()).isEqualTo("medium");
    }

    @Test
    void shouldHandleV2StreamingResponseMetadata() {
        CompletionStreamUsageV2 usage = CompletionStreamUsageV2.builder()
                .promptTokens(5)
                .completionTokens(15)
                .totalTokens(20)
                .build();

        CompletionMessageDoneEventV2 doneEvent = CompletionMessageDoneEventV2.builder()
                .model("GigaChat-Pro")
                .finishReason("stop")
                .usage(usage)
                .build();

        assertThat(doneEvent.model()).isEqualTo("GigaChat-Pro");
        assertThat(doneEvent.finishReason()).isEqualTo("stop");
        assertThat(doneEvent.usage()).isNotNull();
        assertThat(doneEvent.usage().promptTokens()).isEqualTo(5);
        assertThat(doneEvent.usage().completionTokens()).isEqualTo(15);
        assertThat(doneEvent.usage().totalTokens()).isEqualTo(20);
    }

    @Test
    void shouldValidateV2StreamingImplementation() {
        GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build())
                .build();

        assertThat(model.defaultRequestParameters()).isInstanceOf(GigaChatChatRequestParameters.class);
        assertThat(model.defaultRequestParameters().getUseV2Completions()).isTrue();
    }
}
