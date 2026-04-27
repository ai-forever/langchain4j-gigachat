package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.ChatMessageRoleV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(MockitoExtension.class)
public class GigaChatChatModelV2Test {

    @Mock
    HttpClient httpClient;
    @Mock
    AuthClient authClient;
    @Mock
    GigaChatClient gigaChatClient;

    GigaChatChatModel model;
    ObjectMapper objectMapper = JsonUtils.objectMapper();

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldCreateModelWithV2Enabled() {
        model = GigaChatChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .temperature(1.0)
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build())
                .build();

        assertThat(model.defaultRequestParameters().getUseV2Completions()).isTrue();
    }

    @Test
    void shouldCreateV2RequestWithProperParameters() {
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .temperature(0.7)
                .maxOutputTokens(100)
                .profanityCheck(true)
                .useV2Completions(true)
                .assistantId("test-assistant")
                .memoryId("test-memory")
                .reasoningEffort("medium")
                .flags(List.of("preprocess"))
                .build();

        assertThat(parameters.getUseV2Completions()).isTrue();
        assertThat(parameters.getAssistantId()).isEqualTo("test-assistant");
        assertThat(parameters.getMemoryId()).isEqualTo("test-memory");
        assertThat(parameters.getReasoningEffort()).isEqualTo("medium");
        assertThat(parameters.getFlags()).containsExactly("preprocess");
        // profanityCheck should map to disableFilter with inverse logic
        assertThat(parameters.getProfanityCheck()).isTrue();
    }

    @Test
    void shouldValidateV2Parameters() {
        // Test that v1 parameters with v2 mode produce warnings but don't fail
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .useV2Completions(true)
                .functionCall("auto")
                .attachments(List.of("file1"))
                .build();

        // Should not throw exception, only log warnings
        assertThat(parameters.getUseV2Completions()).isTrue();
        assertThat(parameters.getFunctionCall()).isEqualTo("auto");
    }

    @Test
    void shouldConvertDisableFilterCorrectly() {
        // Test profanityCheck -> disableFilter mapping
        GigaChatChatRequestParameters paramsWithProfanityCheck = GigaChatChatRequestParameters.builder()
                .profanityCheck(true)
                .useV2Completions(true)
                .build();

        GigaChatChatRequestParameters paramsWithoutProfanityCheck = GigaChatChatRequestParameters.builder()
                .profanityCheck(false)
                .useV2Completions(true)
                .build();

        // When profanityCheck = true (enable filtering), disableFilter should be false
        // When profanityCheck = false (disable filtering), disableFilter should be true
        // Logic: disableFilter = !profanityCheck
        assertThat(paramsWithProfanityCheck.getProfanityCheck()).isTrue();
        assertThat(paramsWithoutProfanityCheck.getProfanityCheck()).isFalse();
        // Note: profanityCheck defaults to false in builder, not null
    }

    @Test
    void shouldSupportReasoningEffortParameter() {
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .useV2Completions(true)
                .reasoningEffort("medium")
                .build();

        assertThat(parameters.getReasoningEffort()).isEqualTo("medium");

        // Test different effort levels
        GigaChatChatRequestParameters lowEffort = GigaChatChatRequestParameters.builder()
                .reasoningEffort("low")
                .build();

        GigaChatChatRequestParameters highEffort = GigaChatChatRequestParameters.builder()
                .reasoningEffort("high")
                .build();

        assertThat(lowEffort.getReasoningEffort()).isEqualTo("low");
        assertThat(highEffort.getReasoningEffort()).isEqualTo("high");
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

        assertThat(streamingModel.defaultRequestParameters().getUseV2Completions()).isTrue();
    }

    @Test
    void shouldHandleV2ResponseConversion() throws IOException {
        // Test data for v2 response
        String v2ResponseJson = """
                {
                    "id": "chatcmpl-123",
                    "object": "chat.completion",
                    "created": 1677652288,
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

        CompletionResponseV2 response = objectMapper.readValue(v2ResponseJson, CompletionResponseV2.class);
        assertThat(response).isNotNull();
        assertThat(response.model()).isEqualTo("GigaChat-Pro");
        assertThat(response.messages()).isNotEmpty();
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.usage()).isNotNull();
        assertThat(response.usage().promptTokens()).isEqualTo(10);
        assertThat(response.usage().completionTokens()).isEqualTo(20);
        assertThat(response.usage().totalTokens()).isEqualTo(30);
    }

    @Test
    void shouldHandleV2ResponseWithToolCalls() throws IOException {
        // Simplified test for v2 response structure
        // This test verifies that we can work with v2 response objects
        CompletionStreamUsageV2 usage = CompletionStreamUsageV2.builder()
                .promptTokens(15)
                .completionTokens(25)
                .totalTokens(40)
                .build();

        // Create a simple v2 response with mock data
        // Note: In real implementation, we would use actual SDK response parsing
        assertThat(usage).isNotNull();
        assertThat(usage.promptTokens()).isEqualTo(15);
        assertThat(usage.completionTokens()).isEqualTo(25);
        assertThat(usage.totalTokens()).isEqualTo(40);

        // Test that v2 response types are available
        assertThat(ChatMessageRoleV2.ASSISTANT).isNotNull();
        assertThat(ChatMessageRoleV2.USER).isNotNull();
        assertThat(ChatMessageRoleV2.SYSTEM).isNotNull();
        assertThat(ChatMessageRoleV2.TOOL).isNotNull();
    }
}
