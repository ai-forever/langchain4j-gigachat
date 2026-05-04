package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import chat.giga.util.JsonUtils;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static chat.giga.langchain4j.utils.GigaChatHelperV2.toRequestV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toResponseV2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class GigaChatChatModelV2Test {

    @Mock
    HttpClient httpClient;
    @Mock
    AuthClient authClient;

    @Test
    void shouldCreateModelWithV2Enabled() {
        GigaChatChatModel model = GigaChatChatModel.builder()
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
    void shouldCreateModelWithV2DisabledByDefault() {
        GigaChatChatModel model = GigaChatChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .build();

        assertThat(model.defaultRequestParameters().getUseV2Completions()).isFalse();
    }

    @Test
    void shouldConvertDisableFilterFromProfanityCheck() {
        ChatRequest chatRequestWithProfanity = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
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
                        .profanityCheck(false)
                        .build())
                .build();

        CompletionRequestV2 requestWithoutProfanity = toRequestV2(chatRequestWithoutProfanity);
        assertThat(requestWithoutProfanity.disableFilter()).isFalse();
    }

    @Test
    void shouldPreferDisableFilterOverProfanityCheck() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .disableFilter(true)
                        .profanityCheck(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = toRequestV2(chatRequest);
        assertThat(requestV2.disableFilter()).isTrue();
    }

    @Test
    void shouldIncludeV2SpecificParametersInRequest() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .assistantId("assistant-123")
                        .memoryId("memory-456")
                        .reasoningEffort("medium")
                        .flags(List.of("preprocess"))
                        .build())
                .build();

        CompletionRequestV2 requestV2 = toRequestV2(chatRequest);

        assertThat(requestV2.assistantId()).isEqualTo("assistant-123");
        assertThat(requestV2.memoryId()).isEqualTo("memory-456");
        assertThat(requestV2.modelOptions()).isNotNull();
        assertThat(requestV2.modelOptions().reasoning()).isNotNull();
        assertThat(requestV2.modelOptions().reasoning().effort()).isEqualTo("medium");
        assertThat(requestV2.flags()).containsExactly("preprocess");
    }

    @Test
    void shouldHandleV2ResponseConversion() throws Exception {
        String v2ResponseJson = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "model": "GigaChat-Pro",
                    "messages": [
                        {
                            "role": "assistant",
                            "content": [
                                {
                                    "text": "Hello! How can I help you today?"
                                }
                            ]
                        }
                    ],
                    "finish_reason": "stop",
                    "usage": {
                        "prompt_tokens": 10,
                        "completion_tokens": 20,
                        "total_tokens": 30
                    }
                }
                """;

        CompletionResponseV2 responseV2 = JsonUtils.objectMapper()
                .readValue(v2ResponseJson, CompletionResponseV2.class);
        ChatResponse chatResponse = toResponseV2(responseV2);

        assertThat(chatResponse.aiMessage().text()).isEqualTo("Hello! How can I help you today?");
        assertThat(chatResponse.metadata().modelName()).isEqualTo("GigaChat-Pro");
        assertThat(chatResponse.metadata().finishReason()).isEqualTo(FinishReason.STOP);
        assertThat(chatResponse.metadata().tokenUsage()).isNotNull();
        assertThat(chatResponse.metadata().tokenUsage().inputTokenCount()).isEqualTo(10);
        assertThat(chatResponse.metadata().tokenUsage().outputTokenCount()).isEqualTo(20);
        assertThat(chatResponse.metadata().tokenUsage().totalTokenCount()).isEqualTo(30);
    }

    @Test
    void shouldThrowWhenDefaultParametersNotGigaChatAndNoV2() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(DefaultChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .build())
                .build();

        assertThatThrownBy(() -> toRequestV2(chatRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot convert to v2 request when useV2Completions is false");
    }

    @Test
    void shouldSupportV2StreamingModel() {
        GigaChatStreamingChatModel streamingModel = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build())
                .build();

        assertThat(streamingModel.defaultRequestParameters()).isInstanceOf(GigaChatChatRequestParameters.class);
        assertThat(streamingModel.defaultRequestParameters().getUseV2Completions()).isTrue();
    }
}
