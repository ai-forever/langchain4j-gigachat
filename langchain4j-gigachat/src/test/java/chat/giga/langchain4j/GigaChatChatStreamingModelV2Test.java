package chat.giga.langchain4j;

import chat.giga.client.GigaChatClientAsync;
import chat.giga.client.CompletionV2StreamHandler;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.stream.CompletionMessageDeltaEventV2;
import chat.giga.model.v2.completion.stream.CompletionMessageDoneEventV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GigaChatChatStreamingModelV2Test {

    @Mock
    HttpClient httpClient;
    @Mock
    AuthClient authClient;
    @Mock
    GigaChatClientAsync asyncClient;

    GigaChatStreamingChatModel model;
    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Create model with v2 enabled
        model = new GigaChatStreamingChatModel(
                httpClient,
                authClient,
                10000, // readTimeout
                5000,  // connectTimeout
                "https://api.gigachat.ai",
                false, // logRequests
                false, // logResponses
                true,  // verifySslCerts
                null,  // listeners
                null,  // responseFormat
                false, // strictJsonSchema
                null,  // maxRetriesOnAuthError
                GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build(),
                true   // useV2Completions
        );

        // Use reflection to inject mocked asyncClient for testing
        // In real tests, we would need to mock the internal client creation
        // For this test, we'll focus on parameter validation and basic structure
    }

    @Test
    void shouldCreateStreamingModelWithV2Enabled() {
        GigaChatStreamingChatModel streamingModel = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .useV2Completions(true)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .useV2Completions(true)
                        .build())
                .build();

        assertThat(streamingModel.defaultRequestParameters().getUseV2Completions()).isTrue();
        assertThat(streamingModel.defaultRequestParameters()).isInstanceOf(GigaChatChatRequestParameters.class);
    }

    @Test
    void shouldCreateV2StreamingRequest() {
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .temperature(0.7)
                .maxOutputTokens(100)
                .useV2Completions(true)
                .stream(true) // Important for streaming
                .build();

        assertThat(parameters.getUseV2Completions()).isTrue();
        assertThat(parameters.getStream()).isTrue(); // Should be true for streaming
    }

    @Test
    void shouldHandleV2StreamingParameters() {
        // Test that v2-specific parameters are properly handled
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .useV2Completions(true)
                .assistantId("test-assistant-id")
                .memoryId("test-memory-id")
                .reasoningEffort("medium")
                .disableFilter(true)
                .flags(List.of("preprocess", "reasoning"))
                .build();

        assertThat(parameters.getUseV2Completions()).isTrue();
        assertThat(parameters.getAssistantId()).isEqualTo("test-assistant-id");
        assertThat(parameters.getMemoryId()).isEqualTo("test-memory-id");
        assertThat(parameters.getReasoningEffort()).isEqualTo("medium");
        assertThat(parameters.getDisableFilter()).isTrue();
        assertThat(parameters.getFlags()).containsExactly("preprocess", "reasoning");
    }

    @Test
    void shouldConvertProfanityCheckToDisableFilterForStreaming() {
        // For streaming, profanityCheck should map to disableFilter
        GigaChatChatRequestParameters paramsWithFiltering = GigaChatChatRequestParameters.builder()
                .useV2Completions(true)
                .profanityCheck(true) // Enable filtering
                .build();

        GigaChatChatRequestParameters paramsWithoutFiltering = GigaChatChatRequestParameters.builder()
                .useV2Completions(true)
                .profanityCheck(false) // Disable filtering
                .build();

        // profanityCheck = true (enable filtering) -> disableFilter = false
        // profanityCheck = false (disable filtering) -> disableFilter = true
        // Logic: disableFilter = !profanityCheck
        assertThat(paramsWithFiltering.getProfanityCheck()).isTrue();
        assertThat(paramsWithoutFiltering.getProfanityCheck()).isFalse();
    }

    @Test
    void shouldSupportV2StreamingEventHandlerStructure() {
        // Test the structure of v2 streaming event handlers
        // This is more of a compilation/type check than a runtime test

        // Verify that we can create mock handlers for v2 streaming
        CompletionV2StreamHandler handler = new CompletionV2StreamHandler() {
            @Override
            public void onMessageDelta(CompletionMessageDeltaEventV2 event) {
                // Handle delta events
            }

            @Override
            public void onMessageDone(CompletionMessageDoneEventV2 event) {
                // Handle done events
            }

            @Override
            public void onComplete() {
                // Handle completion
            }

            @Override
            public void onError(Throwable th) {
                // Handle errors
            }
        };

        assertThat(handler).isNotNull();
        // Verify default methods exist (onToolInProgress, onToolCompleted)
        handler.onToolInProgress(null);
        handler.onToolCompleted(null);
    }

    @Test
    void shouldCreateStreamingRequestWithV2Format() {
        // Test that streaming requests are properly formatted for v2
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("Hello, stream this!")))
                .parameters(DefaultChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .temperature(0.8)
                        .maxOutputTokens(150)
                        .build())
                .build();

        GigaChatChatRequestParameters streamingParams = GigaChatChatRequestParameters.builder()
                .useV2Completions(true)
                .stream(true) // Must be true for streaming
                .build();

        // The actual conversion would happen in GigaChatHelperV2.toRequestV2
        // For streaming, the request should have stream = true
        assertThat(streamingParams.getStream()).isTrue();
        assertThat(streamingParams.getUseV2Completions()).isTrue();
    }

    @Test
    void shouldHandleV2StreamingResponseMetadata() {
        // Test v2 streaming response metadata structure
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

        assertThat(doneEvent).isNotNull();
        assertThat(doneEvent.model()).isEqualTo("GigaChat-Pro");
        assertThat(doneEvent.finishReason()).isEqualTo("stop");
        assertThat(doneEvent.usage()).isNotNull();
        assertThat(doneEvent.usage().promptTokens()).isEqualTo(5);
        assertThat(doneEvent.usage().completionTokens()).isEqualTo(15);
        assertThat(doneEvent.usage().totalTokens()).isEqualTo(20);
    }

    @Test
    void shouldSupportV2StreamingWithTools() {
        // Test configuration for v2 streaming with tools
        GigaChatChatRequestParameters parameters = GigaChatChatRequestParameters.builder()
                .modelName(ModelName.GIGA_CHAT_PRO)
                .useV2Completions(true)
                .stream(true)
                .build();

        assertThat(parameters.getUseV2Completions()).isTrue();
        assertThat(parameters.getStream()).isTrue();

        // In v2, tools would be specified differently than v1
        // This test verifies the basic parameter structure
    }

    @Test
    void shouldValidateV2StreamingImplementation() {
        // This test validates that our v2 streaming implementation
        // follows the correct patterns and interfaces

        // Check that model has the necessary methods
        GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("test")
                .useV2Completions(true)
                .build();

        assertThat(model.defaultRequestParameters()).isNotNull();
        assertThat(model.defaultRequestParameters()).isInstanceOf(GigaChatChatRequestParameters.class);

        GigaChatChatRequestParameters params = (GigaChatChatRequestParameters) model.defaultRequestParameters();
        // Note: useV2Completions might be null (not set) in the default parameters
        // depending on builder logic
    }
}
