package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.ModelName;
import chat.giga.model.v2.completion.ChatMessageRoleV2;
import chat.giga.model.v2.completion.ChatMessageV2;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;

import java.util.List;

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
                        .modelName(ModelName.GIGA_CHAT_PRO)
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
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .temperature(0.7)
                        .maxOutputTokens(100)
                        .useV2Completions(true)
                        .profanityCheck(true)
                        .build())
                .build();

        CompletionRequestV2 requestV2 = GigaChatHelperV2.toRequestV2(chatRequest);

        assertThat(requestV2).isNotNull();
        assertThat(requestV2.model()).isEqualTo(ModelName.GIGA_CHAT_PRO);
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
}
