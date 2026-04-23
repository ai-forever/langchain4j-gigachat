package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
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
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
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
        assertEquals(0, response.aiMessage().toolExecutionRequests().size());
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
        assertEquals(0, response.aiMessage().toolExecutionRequests().size());
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
    void testConvertToChatFunctionParametersWithFewShotExamples() throws JsonProcessingException {

        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonObjectSchema.builder().build()))
                                .build())
                        .name("test")
                        .metadata(Map.of("few_shot_examples", JsonUtils.objectMapper().readTree("""
                                [
                                    {
                                        "request": "Отправь смс на номер",
                                        "params" : {
                                                       "recipient": "+79683331211",
                                                       "message": "Как Дела"
                                                   }
                                    }
                                ]
                                """)))
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);

        assertThat(request.functions().get(0).fewShotExamples().get(0)).satisfies(
                cr -> {

                    assertThat(cr.request()).isEqualTo("Отправь смс на номер");
                    assertThat(cr.params().get("recipient")).isEqualTo("+79683331211");
                    assertThat(cr.params().get("message")).isEqualTo("Как Дела");
                }
        );
    }

    @Test
    void testConvertToChatFunctionParametersWithReturnParameters() throws JsonProcessingException {

        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonObjectSchema.builder().build()))
                                .build())
                        .name("test")
                        .metadata(Map.of("return_parameters", JsonUtils.objectMapper().readTree("""
                                {
                                    "type": "object",
                                    "properties": {
                                        "status": {
                                            "type": "string",
                                            "description": "Статус отправки Ok или Error"
                                        }
                                    }
                                }
                                """)))
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);

        assertThat(request.functions().get(0).returnParameters()).satisfies(
                cr -> {
                    assertThat(cr.type()).isEqualTo("object");
                    assertThat(cr.properties().get("status").type()).isEqualTo("string");
                    assertThat(cr.properties().get("status").description()).isEqualTo("Статус отправки Ok или Error");
                }
        );
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonArraySchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonArraySchema.builder()
                                        .description("testArray")
                                        .items(JsonStringSchema.builder().description("testval").build())
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        var props = request.functions().get(0).parameters().properties();
        assertNotNull(props);
        assertThat(props).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("array"));
        assertThat(props).satisfies(
                cr -> assertThat(cr.get("key").description()).isEqualTo("testArray"));
        assertThat(props).satisfies(
                cr -> assertThat(cr.get("key").items().get("type")).isEqualTo("string"));
    }


    @Test
    void testConvertToChatFunctionParametersWithJsonEnumSchema() {
        List<String> enums = List.of("enum1", "enum2", "enum3");
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonEnumSchema.builder()
                                        .description("testDescription")
                                        .enumValues(enums)
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("string"));
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").enums()).isEqualTo(enums));
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonBooleanSchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonBooleanSchema.builder()
                                        .description("testDescription")
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("boolean"));
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonIntegerSchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonIntegerSchema.builder()
                                        .description("testDescription")
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("integer"));
    }

    @Test
    void testConvertToChatFunctionParametersWithJsonNumberSchema() {
        var chatRequest = chatRequest()
                .toolSpecifications(ToolSpecification.builder()
                        .parameters(JsonObjectSchema.builder()
                                .addProperties(Map.of("key", JsonNumberSchema.builder()
                                        .description("testDescription")
                                        .build()))
                                .build())
                        .name("test")
                        .build())
                .parameters(null).build();
        CompletionRequest request = GigaChatHelper.toRequest(chatRequest);
        assertThat(request.functions().get(0).parameters().properties()).satisfies(
                cr -> assertThat(cr.get("key").type()).isEqualTo("number"));
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

    @Test
    void testToRequestWithStructOutputJsonFormat() throws JsonProcessingException {
        String raw_schema = """
                {"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"}},"required":["name","age"]}}
                """;
        ResponseFormat responseFormatRaw = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("Person")
                        .rootElement(JsonObjectSchema.builder()
                                .addStringProperty("name")
                                .addIntegerProperty("age")
                                .required("name", "age")
                                .build())
                        .build())
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(
                        GigaChatChatRequestParameters.builder().modelName("testModel").responseFormat(responseFormatRaw)
                                .strictJsonSchema(false).build())
                .messages(UserMessage.from("hello"))
                .build());

        assertNotNull(request);
        assertNotNull(request.responseFormat());
        chat.giga.model.completion.ResponseFormat responseFormatConverted = request.responseFormat();
        assertEquals(chat.giga.model.completion.ResponseFormatType.JSON_SCHEMA, responseFormatConverted.type());
        assertEquals(JsonUtils.objectMapper().readTree(raw_schema),
                JsonUtils.objectMapper().convertValue(responseFormatConverted.schema(), JsonNode.class));

    }

    @Test
    void testToRequestWithStructOutputStringRawFormat() throws JsonProcessingException {
        String raw_schema = """
                {"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"}},"required":["name","age"],"additionalProperties":false}}
                """;
        ResponseFormat responseFormatRaw = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("Person")
                        .rootElement(JsonRawSchema.builder().schema(raw_schema).build())
                        .build())
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(
                        GigaChatChatRequestParameters.builder().modelName("testModel").responseFormat(responseFormatRaw)
                                .strictJsonSchema(false).build())
                .messages(UserMessage.from("hello"))
                .build());

        assertNotNull(request);
        chat.giga.model.completion.ResponseFormat responseFormatConverted = request.responseFormat();
        assertEquals(chat.giga.model.completion.ResponseFormatType.JSON_SCHEMA, responseFormatConverted.type());
        assertEquals(JsonUtils.objectMapper().readTree(raw_schema),
                JsonUtils.objectMapper().convertValue(responseFormatConverted.schema(), JsonNode.class));
    }

    @Test
    void testToRequestWithJsonAnyOfSchemaToolParameter() {
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("name")
                .addProperty("value", JsonAnyOfSchema.builder()
                        .anyOf(List.of(
                                JsonStringSchema.builder().build(),
                                JsonIntegerSchema.builder().build()))
                        .build())
                .required("name")
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(UserMessage.from("hello"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("testFunction")
                        .parameters(objectSchema)
                        .build())
                .build());

        assertNotNull(request);
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        chat.giga.model.completion.ChatFunction function = request.functions().get(0);
        assertEquals("testFunction", function.name());
        assertNotNull(function.parameters());
        assertNotNull(function.parameters().properties());
        assertNotNull(function.parameters().properties().get("value"));
        assertNotNull(function.parameters().properties().get("value").anyOf());
        assertEquals(2, function.parameters().properties().get("value").anyOf().size());
        assertEquals("string", function.parameters().properties().get("value").anyOf().get(0).type());
        assertEquals("integer", function.parameters().properties().get("value").anyOf().get(1).type());
    }

    @Test
    void testToRequestWithJsonRawSchemaToolParameter() {
        String rawPropSchema = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}";
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("id")
                .addProperty("data", JsonRawSchema.builder().schema(rawPropSchema).build())
                .required("id")
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .messages(UserMessage.from("hello"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("testFunction")
                        .parameters(objectSchema)
                        .build())
                .build());

        assertNotNull(request);
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        chat.giga.model.completion.ChatFunction function = request.functions().get(0);
        assertEquals("testFunction", function.name());
        assertNotNull(function.parameters().properties().get("data"));
        assertEquals("object", function.parameters().properties().get("data").type());
        assertNotNull(function.parameters().properties().get("data").properties());
        assertNotNull(function.parameters().properties().get("data").properties().get("name"));
        assertEquals("string", function.parameters().properties().get("data").properties().get("name").type());
        assertNotNull(function.parameters().properties().get("data").properties().get("age"));
        assertEquals("integer", function.parameters().properties().get("data").properties().get("age").type());
    }

    @Test
    void testToRequestWithAnyOfSchemaAsResponseFormatRoot() throws JsonProcessingException {
        ResponseFormat responseFormat = ResponseFormat.builder()
                .type(JSON)
                .jsonSchema(JsonSchema.builder()
                        .name("Result")
                        .rootElement(JsonAnyOfSchema.builder()
                                .anyOf(List.of(
                                        JsonObjectSchema.builder()
                                                .addStringProperty("text")
                                                .build(),
                                        JsonObjectSchema.builder()
                                                .addIntegerProperty("number")
                                                .build()))
                                .build())
                        .build())
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName("testModel")
                        .responseFormat(responseFormat)
                        .strictJsonSchema(false)
                        .build())
                .messages(UserMessage.from("hello"))
                .build());

        assertNotNull(request);
        assertNotNull(request.responseFormat());
        assertEquals(chat.giga.model.completion.ResponseFormatType.JSON_SCHEMA, request.responseFormat().type());
        assertNotNull(request.responseFormat().schema());
    }


    @Test
    void testToRequestWithInvalidJsonRawSchema() {
        String invalidJson = "{invalid json}";
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("id")
                .addProperty("data", JsonRawSchema.builder().schema(invalidJson).build())
                .required("id")
                .build();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                GigaChatHelper.toRequest(ChatRequest.builder()
                        .parameters(GigaChatChatRequestParameters.builder()
                                .modelName("testModel")
                                .build())
                        .messages(UserMessage.from("hello"))
                        .toolSpecifications(ToolSpecification.builder()
                                .name("testFunction")
                                .parameters(objectSchema)
                                .build())
                        .build()));

        assertThat(exception.getMessage()).contains("Failed to parse JSON");
    }

    @Test
    void testToRequestWithJsonRawSchemaItemsField() {
        String rawSchemaWithItems = "{\"type\":\"array\",\"items\":{\"type\":\"string\",\"description\":\"A string item\"}}";
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("id")
                .addProperty("arrayData", JsonRawSchema.builder().schema(rawSchemaWithItems).build())
                .required("id")
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName("testModel")
                        .build())
                .messages(UserMessage.from("hello"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("testFunction")
                        .parameters(objectSchema)
                        .build())
                .build());

        assertNotNull(request);
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        chat.giga.model.completion.ChatFunction function = request.functions().get(0);
        assertEquals("testFunction", function.name());
        assertNotNull(function.parameters().properties().get("arrayData"));
        assertEquals("array", function.parameters().properties().get("arrayData").type());
        assertNotNull(function.parameters().properties().get("arrayData").items());
        assertEquals(1, function.parameters().properties().get("arrayData").items().size());
        assertNotNull(function.parameters().properties().get("arrayData").items().get("type"));
        assertEquals("string", function.parameters().properties().get("arrayData").items().get("type"));
    }

    @Test
    void testToRequestWithJsonRawSchemaEnumField() {
        String rawSchemaWithEnum = "{\"type\":\"string\",\"enum\":[\"option1\",\"option2\",\"option3\"]}";
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("id")
                .addProperty("choice", JsonRawSchema.builder().schema(rawSchemaWithEnum).build())
                .required("id")
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName("testModel")
                        .build())
                .messages(UserMessage.from("hello"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("testFunction")
                        .parameters(objectSchema)
                        .build())
                .build());

        assertNotNull(request);
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        chat.giga.model.completion.ChatFunction function = request.functions().get(0);
        assertEquals("testFunction", function.name());
        assertNotNull(function.parameters().properties().get("choice"));
        assertEquals("string", function.parameters().properties().get("choice").type());
        assertNotNull(function.parameters().properties().get("choice").enums());
        assertEquals(3, function.parameters().properties().get("choice").enums().size());
        assertThat(function.parameters().properties().get("choice").enums())
                .containsExactly("option1", "option2", "option3");
    }

    @Test
    void testToRequestWithDeeplyNestedJsonRawSchema() {
        String deepNestedSchema = "{\"type\":\"object\",\"properties\":{\"level1\":{\"type\":\"object\",\"properties\":{\"level2\":{\"type\":\"object\",\"properties\":{\"level3\":{\"type\":\"string\"}}}}}}}";
        JsonObjectSchema objectSchema = JsonObjectSchema.builder()
                .addStringProperty("id")
                .addProperty("nestedData", JsonRawSchema.builder().schema(deepNestedSchema).build())
                .required("id")
                .build();

        CompletionRequest request = GigaChatHelper.toRequest(ChatRequest.builder()
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName("testModel")
                        .build())
                .messages(UserMessage.from("hello"))
                .toolSpecifications(ToolSpecification.builder()
                        .name("testFunction")
                        .parameters(objectSchema)
                        .build())
                .build());

        assertNotNull(request);
        assertNotNull(request.functions());
        assertEquals(1, request.functions().size());
        chat.giga.model.completion.ChatFunction function = request.functions().get(0);
        assertEquals("testFunction", function.name());

        // Check nested structure
        var nestedProp = function.parameters().properties().get("nestedData");
        assertNotNull(nestedProp);
        assertEquals("object", nestedProp.type());

        var level1 = nestedProp.properties().get("level1");
        assertNotNull(level1);
        assertEquals("object", level1.type());

        var level2 = level1.properties().get("level2");
        assertNotNull(level2);
        assertEquals("object", level2.type());

        var level3 = level2.properties().get("level3");
        assertNotNull(level3);
        assertEquals("string", level3.type());
    }
}
