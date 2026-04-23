package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.completion.ChatFunction;
import chat.giga.model.completion.ChatFunctionParameters;
import chat.giga.model.completion.ChatFunctionParametersProperty;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.ChatMessageRole;
import chat.giga.model.completion.ChoiceChunk;
import chat.giga.model.completion.ChoiceMessageFunctionCall;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import chat.giga.model.completion.ResponseFormatType;
import chat.giga.model.completion.Usage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
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
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.request.json.JsonAnyOfSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.JsonSchemaElementUtils.toMap;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;
import static dev.langchain4j.model.chat.request.ResponseFormatType.TEXT;
import static dev.langchain4j.model.output.FinishReason.CONTENT_FILTER;
import static dev.langchain4j.model.output.FinishReason.LENGTH;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION;

public class GigaChatHelper {

    private static final int MAX_RECURSION_DEPTH = 50;

    public enum ParamType {
        OBJECT,
        ARRAY,
        STRING,
        BOOLEAN,
        INTEGER,
        NUMBER;

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public static ChatResponse toResponse(CompletionResponse completions) {
        return completions.choices()
                .stream()
                .map(s -> {
                    ToolExecutionRequest toolExecutionRequest = null;
                    if (s.message().functionCall() != null) {
                        var args = toArgumentsString(s.message().functionCall());
                        toolExecutionRequest = ToolExecutionRequest.builder()
                                .id(s.message().functionsStateId())
                                .name(s.message().functionCall().name())
                                .arguments(args)
                                .build();
                    }
                    var aiMessage = toolExecutionRequest != null ? AiMessage.builder()
                                                                   .text(s.message().content())
                                                                   .toolExecutionRequests(Collections.singletonList(
                                                                           toolExecutionRequest))
                                                                   .build()
                            : AiMessage.builder()
                              .text(s.message().content())
                              .build();
                    return ChatResponse.builder()
                            .aiMessage(aiMessage)
                            .metadata(ChatResponseMetadata.builder()
                                    .modelName(completions.model())
                                    .tokenUsage(new TokenUsage(
                                            completions.usage().promptTokens(),
                                            completions.usage().completionTokens(),
                                            completions.usage().totalTokens()))
                                    .finishReason(finishReasonFrom(
                                            s.finishReason() != null ? s.finishReason().value() : null))
                                    .build())
                            .build();
                })
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Choices is empty in the response"));
    }

    public static CompletionRequest toRequest(ChatRequest chatRequest) {
        GigaChatChatRequestParameters parameters = null;
        if (chatRequest.parameters() instanceof GigaChatChatRequestParameters gigaChatParameters) {
            parameters = gigaChatParameters;
        }
        return CompletionRequest.builder()
                .model(chatRequest.parameters().modelName())
                .messages(convertChatMessages(chatRequest.messages(), parameters))
                .temperature(chatRequest.parameters().temperature() != null ? chatRequest.parameters().temperature()
                                                                              .floatValue() : null)
                .topP(chatRequest.parameters().topP() != null ? chatRequest.parameters().topP().floatValue() : null)
                .maxTokens(chatRequest.parameters().maxOutputTokens())
                .repetitionPenalty(parameters != null ? parameters.getRepetitionPenalty() : null)
                .profanityCheck(parameters != null ? parameters.getProfanityCheck() : null)
                .stream(parameters != null ? parameters.getStream() : null)
                .updateInterval(parameters != null ? parameters.getUpdateInterval() : null)
                .functionCall(parameters != null ? parameters.getFunctionCall() : null)
                .responseFormat(toResponseFormat(chatRequest.responseFormat(),
                        parameters != null ? parameters.getStrictJsonSchema() : false))
                .functions(chatRequest.toolSpecifications() != null ? (
                                chatRequest.toolSpecifications()
                                .stream()
                                .map(toolSpecification -> {
                                    ChatFunctionParameters chatFunctionParameters;
                                    if (toolSpecification.parameters() == null) {
                                        chatFunctionParameters = ChatFunctionParameters.builder()
                                                                 .type(ParamType.OBJECT.toString())
                                                                 .properties(Map.of())
                                                                 .build();
                                    } else {
                                        chatFunctionParameters = ChatFunctionParameters.builder()
                                                                 .required(toolSpecification.parameters().required())
                                                                 .properties(convertParameters(
                                                                         toolSpecification.parameters().properties()))
                                                                 .build();
                                    }
                                    return ChatFunction.builder()
                                           .name(toolSpecification.name())
                                           .description(toolSpecification.description())
                                           .parameters(chatFunctionParameters)
                                           .fewShotExamples(getMetadataValue(
                                                   toolSpecification, "few_shot_examples",
                                                   new TypeReference<>() {
                                                   }, List.of()))
                                           .returnParameters(
                                                   getMetadataValue(toolSpecification, "return_parameters",
                                                           new TypeReference<>() {
                                                           }, null))
                                           .build();
                                })
                                .collect(Collectors.toList())
                        ) : List.of()
                )
                .build();
    }

    private static <T> T getMetadataValue(ToolSpecification toolSpecification, String key,
            TypeReference<T> typeReference, T defaultValue) {
        try {
            Object value = toolSpecification.metadata().get(key);
            if (value != null) {
                return JsonUtils.objectMapper().convertValue(value, typeReference);
            }
            return defaultValue;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to extract " + key + " parameter", ex);
        }
    }

    public static chat.giga.model.completion.ResponseFormat toResponseFormat(ResponseFormat responseFormat,
            Boolean strict) {
        if (responseFormat != null && responseFormat.type() == JSON) {
            JsonSchema jsonSchema = responseFormat.jsonSchema();
            if (jsonSchema != null) {
                if (!(jsonSchema.rootElement() instanceof JsonObjectSchema
                        || jsonSchema.rootElement() instanceof JsonRawSchema
                        || jsonSchema.rootElement() instanceof JsonAnyOfSchema)) {
                    throw new IllegalArgumentException(
                            "For GigaChat, the root element of the JSON Schema must be either a JsonObjectSchema, a JsonRawSchema, or a JsonAnyOfSchema, but it was: "
                                    + (jsonSchema.rootElement() != null ? jsonSchema.rootElement().getClass()
                                    : "null"));
                }
                Map<String, Object> schema = toMap(jsonSchema.rootElement(), strict);
                return chat.giga.model.completion.ResponseFormat.builder()
                        .type(ResponseFormatType.JSON_SCHEMA)
                        .schema(schema)
                        .strict(strict)
                        .build();
            } else {
                throw new IllegalArgumentException("For GigaChat, jsonSchema is missing");
            }
        } else if (responseFormat != null && responseFormat.type() == TEXT) {
            return chat.giga.model.completion.ResponseFormat.builder()
                    .type(ResponseFormatType.TEXT)
                    .build();
        } else {
            return null;
        }
    }

    public static ToolExecutionRequest toToolExecutionRequest(ChoiceChunk choice) {
        ChoiceMessageFunctionCall function = choice.delta().functionCall();
        String functionId = null;
        String functionName = null;
        String functionArguments = null;

        if (choice.delta().functionsStateId() != null) {
            functionId = choice.delta().functionsStateId();
        }
        if (function != null) {
            if (function.name() != null) {
                functionName = function.name();
            }
            if (function.arguments() != null && !function.arguments().isEmpty()) {
                functionArguments = toArgumentsString(function);
            }
        }

        return ToolExecutionRequest.builder()
                .id(functionId)
                .name(functionName)
                .arguments(functionArguments)
                .build();
    }

    public static TokenUsage toTokenUsage(Usage usage) {
        return new TokenUsage(
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens());
    }

    public static FinishReason finishReasonFrom(String reason) {
        if (reason == null) {
            return null;
        }
        return switch (reason) {
            case "stop" -> STOP;
            case "length" -> LENGTH;
            case "function_call" -> TOOL_EXECUTION;
            case "content_filter" -> CONTENT_FILTER;
            default -> null;
        };
    }

    private static List<ChatMessage> convertChatMessages(List<dev.langchain4j.data.message.ChatMessage> messages,
            GigaChatChatRequestParameters parameters) {
        return messages.stream()
                .map(message -> convertMessage(message, parameters))
                .collect(Collectors.toList());
    }

    private static ChatMessage convertMessage(dev.langchain4j.data.message.ChatMessage message,
            GigaChatChatRequestParameters parameters) {
        if (message instanceof UserMessage userMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessageRole.USER)
                    .content(userMessage.contents().stream()
                            .map(content -> content instanceof TextContent ? ((TextContent) content).text() : null)
                            .toList().get(0))
                    .attachments(parameters != null ? getOrDefault(parameters.getAttachments(), List.of()) : List.of())
                    .build();
        } else if (message instanceof SystemMessage systemMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessageRole.SYSTEM)
                    .content(systemMessage.text())
                    .build();
        } else if (message instanceof AiMessage aiMessage) {
            var id = aiMessage.toolExecutionRequests() != null && !aiMessage.toolExecutionRequests().isEmpty() ?
                    aiMessage.toolExecutionRequests().get(0).id() : null;
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessageRole.ASSISTANT)
                    .functionsStateId(id)
                    .content(aiMessage.text())
                    .build();
        } else if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessageRole.FUNCTION)
                    .content(toolExecutionResultMessage.text())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
        }
    }

    private static Map<String, ChatFunctionParametersProperty> convertParameters(
            Map<String, JsonSchemaElement> inputMap) {
        return inputMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToChatFunctionParametersProperty(entry.getValue())
                ));
    }

    private static ChatFunctionParametersProperty convertToChatFunctionParametersProperty(
            JsonSchemaElement schemaElement) {
        if (schemaElement instanceof JsonObjectSchema jsonObjectSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.OBJECT.toString())
                    .properties(convertParameters(jsonObjectSchema.properties()))
                    .description(jsonObjectSchema.description())
                    .build();
        } else if (schemaElement instanceof JsonStringSchema jsonStringSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.STRING.toString())
                    .description(jsonStringSchema.description())
                    .build();
        } else if (schemaElement instanceof JsonIntegerSchema jsonIntegerSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.INTEGER.toString())
                    .description(jsonIntegerSchema.description())
                    .build();
        } else if (schemaElement instanceof JsonNumberSchema jsonNumberSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.NUMBER.toString())
                    .description(jsonNumberSchema.description())
                    .build();
        } else if (schemaElement instanceof JsonEnumSchema jsonEnumSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.STRING.toString())
                    .description(jsonEnumSchema.description())
                    .enums(jsonEnumSchema.enumValues())
                    .build();
        } else if (schemaElement instanceof JsonBooleanSchema jsonBooleanSchema) {
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.BOOLEAN.toString())
                    .description(jsonBooleanSchema.description())
                    .build();
        } else if (schemaElement instanceof JsonArraySchema jsonArraySchema) {
            Map<String, Object> itemsMap;
            ChatFunctionParametersProperty parametersProperty = convertToChatFunctionParametersProperty(
                    jsonArraySchema.items());
            if (parametersProperty.type().equals(ParamType.OBJECT.toString())) {
                itemsMap = Map.of("type", ParamType.OBJECT.toString(), "properties", parametersProperty.properties());
            } else if (parametersProperty.enums() != null && !parametersProperty.enums().isEmpty()) {
                itemsMap = Map.of("type", parametersProperty.type(), "enums", parametersProperty.enums());
            } else {
                itemsMap = Map.of("type", parametersProperty.type());
            }
            return ChatFunctionParametersProperty.builder()
                    .type(ParamType.ARRAY.toString())
                    .description(jsonArraySchema.description())
                    .items(itemsMap)
                    .build();
        } else if (schemaElement instanceof JsonAnyOfSchema jsonAnyOfSchema) {
            List<JsonSchemaElement> anyOfList = jsonAnyOfSchema.anyOf();
            if (anyOfList == null) {
                return ChatFunctionParametersProperty.builder()
                        .description(jsonAnyOfSchema.description())
                        .build();
            }
            return ChatFunctionParametersProperty.builder()
                    .description(jsonAnyOfSchema.description())
                    .anyOf(anyOfList.stream()
                            .map(GigaChatHelper::convertToChatFunctionParametersProperty)
                            .collect(Collectors.toList()))
                    .build();
        } else if (schemaElement instanceof JsonRawSchema jsonRawSchema) {
            return convertRawSchemaToProperty(jsonRawSchema);
        }
        return ChatFunctionParametersProperty.builder().build();
    }

    private static String toArgumentsString(ChoiceMessageFunctionCall function) {
        return JsonUtils.objectMapper().convertValue(function.arguments(), JsonNode.class).toString();
    }

    private static ChatFunctionParametersProperty convertRawSchemaToProperty(JsonRawSchema jsonRawSchema) {
        try {
            ObjectMapper mapper = JsonUtils.objectMapper();
            Map<String, Object> rawMap = mapper.readValue(jsonRawSchema.schema(), new TypeReference<>() {
            });
            ChatFunctionParametersProperty.ChatFunctionParametersPropertyBuilder builder =
                    ChatFunctionParametersProperty.builder();
            if (rawMap.containsKey("type")) {
                Object typeObj = rawMap.get("type");
                if (typeObj != null) {
                    builder.type(typeObj.toString());
                }
            }
            if (rawMap.containsKey("description")) {
                Object descObj = rawMap.get("description");
                if (descObj != null) {
                    builder.description(descObj.toString());
                }
            }
            if (rawMap.containsKey("properties")) {
                Object propertiesObj = rawMap.get("properties");
                if (propertiesObj instanceof Map<?, ?> rawPropsMap) {
                    Map<String, ChatFunctionParametersProperty> convertedProps = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : rawPropsMap.entrySet()) {
                        if (entry.getKey() instanceof String key && entry.getValue() instanceof Map<?, ?> valueMap) {
                            Map<String, Object> typedValueMap = toStringObjectMap(valueMap);
                            convertedProps.put(key,
                                    convertRawPropertyMapToProperty(typedValueMap, 1));
                        }
                    }
                    builder.properties(convertedProps);
                }
            }
            if (rawMap.containsKey("anyOf")) {
                Object anyOfObj = rawMap.get("anyOf");
                if (anyOfObj instanceof List<?> rawAnyOfList) {
                    List<ChatFunctionParametersProperty> anyOfProps = rawAnyOfList.stream()
                            .filter(item -> item instanceof Map<?, ?>)
                            .map(item -> convertRawPropertyMapToProperty(toStringObjectMap((Map<?, ?>) item), 1))
                            .collect(Collectors.toList());
                    builder.anyOf(anyOfProps);
                }
            }
            return builder.build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse JSON in JsonRawSchema: " + e.getMessage(), e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid type conversion in JsonRawSchema: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse JsonRawSchema: " + e.getMessage(), e);
        }
    }

    private static ChatFunctionParametersProperty convertRawPropertyMapToProperty(Map<String, Object> propMap) {
        return convertRawPropertyMapToProperty(propMap, 0);
    }

    private static ChatFunctionParametersProperty convertRawPropertyMapToProperty(Map<String, Object> propMap,
            int depth) {
        if (depth > MAX_RECURSION_DEPTH) {
            throw new IllegalArgumentException(
                    "Maximum recursion depth (" + MAX_RECURSION_DEPTH + ") exceeded while parsing JSON schema");
        }
        
        ChatFunctionParametersProperty.ChatFunctionParametersPropertyBuilder builder =
                ChatFunctionParametersProperty.builder();
        if (propMap.containsKey("type")) {
            Object typeObj = propMap.get("type");
            if (typeObj != null) {
                builder.type(typeObj.toString());
            }
        }
        if (propMap.containsKey("description")) {
            Object descObj = propMap.get("description");
            if (descObj != null) {
                builder.description(descObj.toString());
            }
        }
        if (propMap.containsKey("enum")) {
            Object enumObj = propMap.get("enum");
            if (enumObj instanceof List<?> rawEnumList) {
                List<String> enumValues = rawEnumList.stream()
                        .filter(item -> item instanceof String)
                        .map(String.class::cast)
                        .collect(Collectors.toList());
                builder.enums(enumValues);
            }
        }
        if (propMap.containsKey("properties")) {
            Object propertiesObj = propMap.get("properties");
            if (propertiesObj instanceof Map<?, ?> rawPropsMap) {
                Map<String, ChatFunctionParametersProperty> convertedProps = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawPropsMap.entrySet()) {
                    if (entry.getKey() instanceof String key && entry.getValue() instanceof Map<?, ?> valueMap) {
                        Map<String, Object> typedValueMap = toStringObjectMap(valueMap);
                        convertedProps.put(key,
                                convertRawPropertyMapToProperty(typedValueMap, depth + 1));
                    }
                }
                builder.properties(convertedProps);
            }
        }
        if (propMap.containsKey("items")) {
            Object itemsObj = propMap.get("items");
            if (itemsObj instanceof Map<?, ?> rawItemsMap) {
                Map<String, Object> itemsMap = toStringObjectMap(rawItemsMap);
                builder.items(itemsMap);
            }
        }
        if (propMap.containsKey("anyOf")) {
            Object anyOfObj = propMap.get("anyOf");
            if (anyOfObj instanceof List<?> rawAnyOfList) {
                List<ChatFunctionParametersProperty> anyOfProps = rawAnyOfList.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(item -> convertRawPropertyMapToProperty(toStringObjectMap((Map<?, ?>) item), depth + 1))
                        .collect(Collectors.toList());
                builder.anyOf(anyOfProps);
            }
        }
        return builder.build();
    }

    private static Map<String, Object> toStringObjectMap(Map<?, ?> rawMap) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}
