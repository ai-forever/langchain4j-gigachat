package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;

import static chat.giga.langchain4j.TestData.chatRequest;
import static chat.giga.langchain4j.TestData.completionChunkNullFieldsResponse;
import static chat.giga.langchain4j.TestData.completionChunkResponse;
import static chat.giga.langchain4j.TestData.completionFunctionCallResponse;
import static chat.giga.langchain4j.TestData.completionNullFinishReasonResponse;
import static chat.giga.langchain4j.TestData.completionResponse;
import static chat.giga.model.completion.ChatMessageRole.FUNCTION;
import static chat.giga.model.completion.ChatMessageRole.SYSTEM;
import static chat.giga.model.completion.ChatMessageRole.USER;
import static dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GigaChatHelperTest {

    @Mock
    private CompletionResponse completionResponse;

    @Test
    void testToResponseWithoutFunctionCall() {
        ChatResponse response = GigaChatHelper.toResponse(completionResponse());

        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertNotNull(response.aiMessage().text());
        assertNull(response.aiMessage().toolExecutionRequests());
        assertNotNull(response.metadata());
        assertEquals("testModel", response.metadata().modelName());
        assertEquals(new TokenUsage(1, 2, 3), response.metadata().tokenUsage());
        assertEquals(FinishReason.STOP, response.metadata().finishReason());
    }

    @Test
    void testToResponseWithFunctionCall() {
        ChatResponse response = GigaChatHelper.toResponse(completionFunctionCallResponse());

        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertNotNull(response.aiMessage().text());
        assertNotNull(response.aiMessage().toolExecutionRequests());
        assertEquals(1, response.aiMessage().toolExecutionRequests().size());
        ToolExecutionRequest toolRequest = response.aiMessage().toolExecutionRequests().get(0);
        assertEquals("841b498c-9ef1-4791-a329-e86c44727327", toolRequest.id());
        assertEquals("testFunction", toolRequest.name());
        assertEquals("{\"key\":\"value\"}", toolRequest.arguments());
        assertNotNull(response.metadata());
        assertEquals("testModel", response.metadata().modelName());
        assertEquals(new TokenUsage(1, 2, 3), response.metadata().tokenUsage());
        assertEquals(TOOL_EXECUTION, response.metadata().finishReason());
    }

    @Test
    void testToResponseWithNullFinishReason() {
        ChatResponse response = GigaChatHelper.toResponse(completionNullFinishReasonResponse());

        assertNotNull(response);
        assertNotNull(response.aiMessage());
        assertEquals("No finish reason", response.aiMessage().text());
        assertNull(response.aiMessage().toolExecutionRequests());
        assertNull(response.metadata().finishReason());
    }

    @Test
    void testToResponseWithEmptyChoicesThrowsException() {
        when(completionResponse.choices()).thenReturn(Collections.emptyList());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> GigaChatHelper.toResponse(completionResponse)
        );
        assertEquals("Choices is empty in the response", exception.
                getMessage());
    }

    @Test
    void testToToolExecutionRequestWithAllFields() {
        ToolExecutionRequest request = GigaChatHelper.toToolExecutionRequest(completionChunkResponse().choices().get(0));

        assertNotNull(request);
        assertEquals("testFunc", request.name());
        assertEquals("{\"testArg\":\"testVal\"}", request.arguments());
    }

    @Test
    void testToToolExecutionRequestWithNullFields() {
        ToolExecutionRequest request = GigaChatHelper.toToolExecutionRequest(completionChunkNullFieldsResponse().choices().get(0));

        assertNotNull(request);
        assertNull(request.id());
        assertNull(request.name());
        assertNull(request.arguments());
    }

    @Test
    void testToRequestWithAllParameters() {
        var chatRequest = chatRequest().build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);

        assertNotNull(request);
        assertEquals("testModel", request.model());
        assertNotNull(request.messages());
        assertEquals(0.7f, request.temperature());
        assertEquals(0.9f, request.topP());
        assertEquals(100, request.maxTokens());
        assertEquals(1.2f, request.repetitionPenalty());
        assertFalse(request.profanityCheck());
        assertFalse(request.stream());
        assertEquals(500, request.updateInterval());
        assertEquals("auto", request.functionCall());
        assertNotNull(request.functions());
        assertEquals(0, request.functions().size());
        assertThat(request.functions()).isEmpty();
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonObjectSchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonObjectSchema.builder().build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("object"));
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonArraySchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonArraySchema.builder()
                                        .description("testArray")
                                        .items(JsonObjectSchema.builder().build())
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertNotNull(request.functions().get(0).parameters().properties());
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonStringSchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonStringSchema.builder()
                                        .description("testDescription")
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("string"));
    }

    @Test
    void testToRequestWithNullParametersToolExecutionRequest() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(null)
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);

        assertNotNull(request);
        assertNull(request.model());
        assertNotNull(request.messages());
        assertNull(request.temperature());
        assertNull(request.topP());
        assertNull(request.maxTokens());
        assertNull(request.repetitionPenalty());
        assertNull(request.updateInterval());
        assertNull(request.functionCall());
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        assertThat(request.functions()).isNotEmpty();
    }

    @Test
    void testToRequestWithNullParameters() {
        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(AiMessage.builder().build())
                .parameters(GigaChatChatRequestParameters.builder().modelName("testModel").build()).build());

        assertNotNull(request);
        assertEquals("testModel", request.model());
        assertNotNull(request.messages());
        assertNull(request.temperature());
        assertNull(request.topP());
        assertNull(request.maxTokens());
        assertNull(request.repetitionPenalty());
        assertFalse(request.profanityCheck());
        assertFalse(request.stream());
        assertNull(request.updateInterval());
        assertNull(request.functionCall());
        assertEquals(Collections.emptyList(), request.functions());
    }

    @Test
    void testToRequestWithSystemMessage() {
        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(new SystemMessage("test system message"))
                .parameters(GigaChatChatRequestParameters.builder().modelName("testModel").build()).build());

        assertNotNull(request);
        assertEquals(SYSTEM, request.messages().get(0).role());
    }

    @Test
    void testToRequestWithUserMessage() {
        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(new UserMessage("test user message"))
                .parameters(GigaChatChatRequestParameters.builder().modelName("testModel").build()).build());

        assertNotNull(request);
        assertEquals(USER, request.messages().get(0).role());
    }

    @Test
    void testToRequestWithToolExecutionResultMessage() {
        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(new ToolExecutionResultMessage("testId", "testToolName", "test tool execution result message"))
                .parameters(GigaChatChatRequestParameters.builder().modelName("testModel").build()).build());

        assertNotNull(request);
        assertEquals(FUNCTION, request.messages().get(0).role());
    }

    @Test
    void testToRequestWithConvertToChatFunction() {
        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(new ToolExecutionResultMessage("testId", "testToolName", "test tool execution result message"))
                .toolSpecifications(ToolSpecification.builder().parameters(JsonObjectSchema.builder().build()).build()).build());

        assertNotNull(request);
        assertEquals(FUNCTION, request.messages().get(0).role());
    }
}
