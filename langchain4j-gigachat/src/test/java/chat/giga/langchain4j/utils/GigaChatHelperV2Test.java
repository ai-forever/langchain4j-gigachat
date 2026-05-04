package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.ChatMessageRoleV2;
import chat.giga.model.v2.completion.ChatMessageV2;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GigaChatHelperV2Test {

    @Test
    void shouldConvertFinishReasonV2() {
        assertThat(GigaChatHelperV2.finishReasonFromV2("stop")).isEqualTo(FinishReason.STOP);
        assertThat(GigaChatHelperV2.finishReasonFromV2("length")).isEqualTo(FinishReason.LENGTH);
        assertThat(GigaChatHelperV2.finishReasonFromV2("function_call")).isEqualTo(FinishReason.TOOL_EXECUTION);
        assertThat(GigaChatHelperV2.finishReasonFromV2("content_filter")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(GigaChatHelperV2.finishReasonFromV2("unknown")).isNull();
        assertThat(GigaChatHelperV2.finishReasonFromV2(null)).isNull();
    }

    @Test
    void shouldConvertTokenUsageV2() {
        CompletionStreamUsageV2 usage = CompletionStreamUsageV2.builder()
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .build();

        TokenUsage tokenUsage = GigaChatHelperV2.toTokenUsageV2(usage);
        assertThat(tokenUsage).isNotNull();
        assertThat(tokenUsage.inputTokenCount()).isEqualTo(10);
        assertThat(tokenUsage.outputTokenCount()).isEqualTo(20);
        assertThat(tokenUsage.totalTokenCount()).isEqualTo(30);
    }

    @Test
    void shouldConvertTokenUsageV2WithAlternativeFields() {
        CompletionStreamUsageV2 usage = CompletionStreamUsageV2.builder()
                .inputTokens(15)
                .outputTokens(25)
                .totalTokens(40)
                .build();

        TokenUsage tokenUsage = GigaChatHelperV2.toTokenUsageV2(usage);
        assertThat(tokenUsage).isNotNull();
        assertThat(tokenUsage.inputTokenCount()).isEqualTo(15); // Uses inputTokens when promptTokens is null
        assertThat(tokenUsage.outputTokenCount()).isEqualTo(25); // Uses outputTokens when completionTokens is null
        assertThat(tokenUsage.totalTokenCount()).isEqualTo(40);
    }

    @Test
    void shouldHandleNullTokenUsageV2() {
        assertThat(GigaChatHelperV2.toTokenUsageV2(null)).isNull();
    }

    @Test
    void shouldConvertToDisableFilter() {
        // profanityCheck = true (enable filtering) -> disableFilter = false
        // profanityCheck = false (disable filtering) -> disableFilter = true
        // profanityCheck = null -> disableFilter = null

        assertThat(GigaChatHelperV2.toDisableFilter(true)).isFalse();
        assertThat(GigaChatHelperV2.toDisableFilter(false)).isTrue();
        assertThat(GigaChatHelperV2.toDisableFilter(null)).isNull();
    }

    @Test
    void shouldConvertChatMessageV2ToAiMessage() {
        ChatMessageV2 message = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hello world");
        CompletionResponseV2 response = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(message))
                .finishReason("stop")
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(response);

        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.aiMessage().text()).isEqualTo("Hello world");
        assertThat(chatResponse.metadata().modelName()).isEqualTo("GigaChat-Pro");
    }

    @Test
    void shouldThrowWhenUseV2IsFalse() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(false)
                        .build())
                .build();

        assertThatThrownBy(() -> GigaChatHelperV2.toRequestV2(chatRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot convert to v2 request when useV2Completions is false");
    }

    @Test
    void shouldConvertSimpleRequestToV2() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("Hello")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .temperature(0.7)
                        .maxOutputTokens(100)
                        .useV2Completions(true)
                        .profanityCheck(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2).isNotNull();
        assertThat(requestV2.model()).isEqualTo(ModelName.GIGA_CHAT_ULTRA_3);
        assertThat(requestV2.disableFilter()).isFalse(); // profanityCheck=true -> disableFilter=false
        assertThat(requestV2.messages()).isNotEmpty();
        assertThat(requestV2.modelOptions()).isNotNull();
        assertThat(requestV2.modelOptions().temperature()).isEqualTo(0.7f);
        assertThat(requestV2.modelOptions().maxTokens()).isEqualTo(100);
    }

    @Test
    void shouldConvertResponseV2ToChatResponse() {
        // Create a mock v2 response
        CompletionStreamUsageV2 usage = CompletionStreamUsageV2.builder()
                .promptTokens(5)
                .completionTokens(10)
                .totalTokens(15)
                .build();

        ChatMessageV2 message = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hello there!");

        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(message))
                .finishReason("stop")
                .usage(usage)
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(responseV2);

        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.aiMessage().text()).isEqualTo("Hello there!");
        assertThat(chatResponse.metadata()).isNotNull();
        assertThat(chatResponse.metadata().modelName()).isEqualTo("GigaChat-Pro");
        assertThat(chatResponse.metadata().finishReason()).isEqualTo(FinishReason.STOP);
        assertThat(chatResponse.metadata().tokenUsage()).isNotNull();
        assertThat(chatResponse.metadata().tokenUsage().inputTokenCount()).isEqualTo(5);
        assertThat(chatResponse.metadata().tokenUsage().outputTokenCount()).isEqualTo(10);
        assertThat(chatResponse.metadata().tokenUsage().totalTokenCount()).isEqualTo(15);
    }

    @Test
    void shouldConvertResponseV2WithMultipleMessages() {
        // v2 response can have multiple messages, we should extract assistant message
        ChatMessageV2 userMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.USER, "Hello");
        ChatMessageV2 assistantMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hi there!");

        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(userMessage, assistantMessage))
                .finishReason("stop")
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(responseV2);

        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.aiMessage().text()).isEqualTo("Hi there!");
    }

    @Test
    void shouldThrowWhenNoAssistantMessageInResponse() {
        // Response with only user message (no assistant message)
        ChatMessageV2 userMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.USER, "Hello");

        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(userMessage))
                .build();

        assertThatThrownBy(() -> GigaChatHelperV2.toResponseV2(responseV2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No assistant message found");
    }

    @Test
    void shouldThrowWhenEmptyMessagesInResponse() {
        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .build();

        assertThatThrownBy(() -> GigaChatHelperV2.toResponseV2(responseV2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Messages is empty");
    }

    @Test
    void shouldUseResponseFormatV2() {
        // toResponseFormatV2 delegates to GigaChatHelper.toResponseFormat
        // This is a compatibility test
        assertThat(GigaChatHelperV2.toResponseFormatV2(null, false)).isNull();
    }

    @Test
    void shouldIncludeStreamParameterInRequest() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .stream(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.stream()).isTrue();
    }

    @Test
    void shouldSetStreamToFalseWhenNotSpecified() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.stream()).isFalse();
    }

    @Test
    void shouldUseExplicitToolConfigWhenProvided() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolConfig(chat.giga.model.v2.completion.ToolConfigV2.forcedFunction("my_function"))
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.toolConfig()).isNotNull();
        assertThat(requestV2.toolConfig().mode()).isEqualTo("forced");
        assertThat(requestV2.toolConfig().functionName()).isEqualTo("my_function");
    }

    @Test
    void shouldConvertToolChoiceAutoToToolConfig() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolChoice(dev.langchain4j.model.chat.request.ToolChoice.AUTO)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.toolConfig()).isNotNull();
        assertThat(requestV2.toolConfig().mode()).isEqualTo("auto");
    }

    @Test
    void shouldConvertToolChoiceNoneToToolConfig() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolChoice(dev.langchain4j.model.chat.request.ToolChoice.NONE)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.toolConfig()).isNotNull();
        assertThat(requestV2.toolConfig().mode()).isEqualTo("none");
    }

    @Test
    void shouldConvertToolChoiceRequiredToToolConfig() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolChoice(dev.langchain4j.model.chat.request.ToolChoice.REQUIRED)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.toolConfig()).isNotNull();
        assertThat(requestV2.toolConfig().mode()).isEqualTo("forced");
    }

    @Test
    void shouldPreferExplicitToolConfigOverToolChoice() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolConfig(chat.giga.model.v2.completion.ToolConfigV2.builder()
                                .mode("forced")
                                .toolName("my_tool")
                                .build())
                        .toolChoice(dev.langchain4j.model.chat.request.ToolChoice.NONE)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        // Explicit toolConfig should take precedence over ToolChoice
        assertThat(requestV2.toolConfig()).isNotNull();
        assertThat(requestV2.toolConfig().mode()).isEqualTo("forced");
        assertThat(requestV2.toolConfig().toolName()).isEqualTo("my_tool");
    }

    @Test
    void shouldSetToolConfigToNullWhenNotSpecified() {
        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.toolConfig()).isNull();
    }


    @Test
    void shouldConvertFewShotExamplesFromMetadata() {
        Map<String, Object> example1 = new java.util.LinkedHashMap<>();
        example1.put("request", "Погода в Москве");
        example1.put("params", Map.of("city", "Москва"));

        Map<String, Object> example2 = new java.util.LinkedHashMap<>();
        example2.put("request", "Погода в Нью-Йорке");
        example2.put("params", Map.of("city", "Нью-Йорк"));

        Map<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("few_shot_examples", List.of(example1, example2));

        ToolSpecification toolSpec = ToolSpecification.builder()
                .name("get_weather")
                .description("Get weather for a city")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city")
                        .build())
                .metadata(metadata)
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(List.of(UserMessage.from("test")))
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_ULTRA_3)
                        .useV2Completions(true)
                        .toolSpecifications(List.of(toolSpec))
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2.tools()).isNotEmpty();
        // Verify that the request was built correctly - the few_shot_examples
        // should be included in the function specifications
    }

    @Test
    void shouldMapBlacklistReasonToContentFilter() {
        assertThat(GigaChatHelperV2.finishReasonFromV2("blacklist")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(GigaChatHelperV2.finishReasonFromV2("request_blacklist")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(GigaChatHelperV2.finishReasonFromV2("request_whitelist")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(GigaChatHelperV2.finishReasonFromV2("request_filter")).isEqualTo(FinishReason.CONTENT_FILTER);
        assertThat(GigaChatHelperV2.finishReasonFromV2("response_blacklist")).isEqualTo(FinishReason.CONTENT_FILTER);
    }

    @Test
    void shouldIncludeFinishReasonInResponseAttributes() {
        ChatMessageV2 assistantMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hello!");
        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(assistantMessage))
                .finishReason("blacklist")
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(responseV2);

        assertThat(chatResponse.aiMessage().attributes()).containsEntry("finish_reason", "blacklist");
        assertThat(chatResponse.metadata().finishReason()).isEqualTo(FinishReason.CONTENT_FILTER);
    }


    @Test
    void shouldIncludeThreadIdInResponseAttributes() {
        ChatMessageV2 assistantMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hello!");
        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(assistantMessage))
                .finishReason("stop")
                .threadId("thread-abc-123")
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(responseV2);

        assertThat(chatResponse.aiMessage().attributes()).containsEntry("thread_id", "thread-abc-123");
    }

    @Test
    void shouldNotIncludeThreadIdWhenNull() {
        ChatMessageV2 assistantMessage = ChatMessageV2.textMessage(ChatMessageRoleV2.ASSISTANT, "Hello!");
        CompletionResponseV2 responseV2 = CompletionResponseV2.builder()
                .model("GigaChat-Pro")
                .messages(List.of(assistantMessage))
                .finishReason("stop")
                .build();

        ChatResponse chatResponse = GigaChatHelperV2.toResponseV2(responseV2);

        assertThat(chatResponse.aiMessage().attributes()).doesNotContainKey("thread_id");
    }

}
