package chat.giga.langchain4j.utils;

import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.model.v2.completion.ChatMessageRoleV2;
import chat.giga.model.v2.completion.ChatMessageV2;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import chat.giga.model.v2.completion.FileRefV2;
import chat.giga.model.v2.completion.FunctionCallContentV2;
import chat.giga.model.v2.completion.FunctionResultContentV2;
import chat.giga.model.v2.completion.FunctionSpecificationV2;
import chat.giga.model.v2.completion.FunctionsToolPayloadV2;
import chat.giga.model.v2.completion.MessageContentPartV2;
import chat.giga.model.v2.completion.ModelOptionsV2;
import chat.giga.model.v2.completion.ReasoningV2;
import chat.giga.model.v2.completion.ToolV2;
import chat.giga.model.v2.completion.stream.CompletionStreamUsageV2;
import chat.giga.util.JsonUtils;
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
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.JsonSchemaElementUtils.toMap;
import static dev.langchain4j.model.output.FinishReason.CONTENT_FILTER;
import static dev.langchain4j.model.output.FinishReason.LENGTH;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION;

/**
 * Вспомогательный класс для конвертации между langchain4j и GigaChat API v2.
 * <p>
 * Предоставляет методы для преобразования запросов и ответов между форматами langchain4j и GigaChat API версии 2.
 * <p>
 * API v2 предоставляет дополнительные возможности, такие как работа с ассистентами, памятью, reasoning-режимом и
 * расширенными настройками модели.
 *
 * @see chat.giga.model.v2.completion.CompletionRequestV2
 * @see chat.giga.model.v2.completion.CompletionResponseV2
 */
public class GigaChatHelperV2 {

    /**
     * Преобразует запрос langchain4j в запрос GigaChat API v2.
     * <p>
     * Метод учитывает специфичные параметры GigaChat и выполняет валидацию совместимости параметров для API v2.
     *
     * @param chatRequest запрос langchain4j
     * @param parameters  специфичные параметры GigaChat
     * @return запрос в формате GigaChat API v2
     * @throws IllegalStateException если {@code useV2Completions} равен {@code false}
     */
    public static CompletionRequestV2 toRequestV2(ChatRequest chatRequest, GigaChatChatRequestParameters parameters) {
        boolean useV2 = parameters != null && Boolean.TRUE.equals(parameters.getUseV2Completions());
        if (!useV2) {
            throw new IllegalStateException("Cannot convert to v2 request when useV2Completions is false");
        }

        // Validate v2-specific parameters
        validateV2Parameters(parameters);

        ModelOptionsV2.ModelOptionsV2Builder modelOptionsBuilder = ModelOptionsV2.builder()
                .temperature(chatRequest.parameters().temperature() != null ? chatRequest.parameters().temperature()
                                                                              .floatValue() : null)
                .topP(chatRequest.parameters().topP() != null ? chatRequest.parameters().topP().floatValue() : null)
                .maxTokens(chatRequest.parameters().maxOutputTokens())
                .repetitionPenalty(parameters != null ? parameters.getRepetitionPenalty() : null)
                .updateInterval(parameters != null && parameters.getUpdateInterval() != null ?
                        parameters.getUpdateInterval().floatValue() : null)
                .responseFormat(toResponseFormatV2(chatRequest.responseFormat(),
                        parameters != null ? parameters.getStrictJsonSchema() : false));

        // Add reasoning if reasoningEffort is set
        if (parameters != null && parameters.getReasoningEffort() != null) {
            ReasoningV2 reasoning = ReasoningV2.builder()
                    .effort(parameters.getReasoningEffort())
                    .build();
            modelOptionsBuilder.reasoning(reasoning);
        }

        ModelOptionsV2 modelOptions = modelOptionsBuilder.build();

        CompletionRequestV2.CompletionRequestV2Builder builder = CompletionRequestV2.builder()
                .model(chatRequest.parameters().modelName())
                .messages(convertChatMessagesV2(chatRequest.messages(), parameters))
                .disableFilter(toDisableFilter(parameters != null ? parameters.getProfanityCheck() : null))
                .assistantId(parameters != null ? parameters.getAssistantId() : null)
                .memoryId(parameters != null ? parameters.getMemoryId() : null)
                .stream(false)
                .modelOptions(modelOptions)
                .tools(convertToolsV2(chatRequest.toolSpecifications()));

        // Add flags only if not null (Lombok builder doesn't accept null for collections)
        if (parameters != null && parameters.getFlags() != null) {
            builder.flags(parameters.getFlags());
        }

        return builder.build();
    }

    private static void validateV2Parameters(GigaChatChatRequestParameters parameters) {
        if (parameters == null) {
            return;
        }

        // Check for v1 parameters that might not be fully compatible with v2
        List<String> warnings = new ArrayList<>();

        if (parameters.getFunctionCall() != null) {
            warnings.add("functionCall parameter may not be fully compatible with v2 API. Use toolConfig instead.");
        }

        if (parameters.getAttachments() != null && !parameters.getAttachments().isEmpty()) {
            warnings.add("attachments parameter may need conversion to v2 file references.");
        }

        if (!warnings.isEmpty()) {
            // Log warnings but don't fail - some parameters might still work
            System.err.println("GigaChat v2 API warnings: " + String.join("; ", warnings));
        }
    }

    /**
     * Преобразует ответ GigaChat API v2 в ответ langchain4j.
     * <p>
     * Извлекает сообщение ассистента из ответа и преобразует его в формат langchain4j,
     * включая информацию об использовании токенов и причине завершения.
     *
     * @param completions ответ от GigaChat API v2
     * @return ответ в формате langchain4j
     * @throws IllegalArgumentException если ответ не содержит сообщений или сообщение ассистента
     */
    public static ChatResponse toResponseV2(CompletionResponseV2 completions) {
        if (completions.messages() == null || completions.messages().isEmpty()) {
            throw new IllegalArgumentException("Messages is empty in the v2 response");
        }

        ChatMessageV2 assistantMessage = findAssistantMessage(completions.messages());
        if (assistantMessage == null) {
            throw new IllegalArgumentException("No assistant message found in v2 response");
        }

        AiMessage aiMessage = convertChatMessageV2ToAiMessage(assistantMessage);

        return ChatResponse.builder()
                .aiMessage(aiMessage)
                .metadata(ChatResponseMetadata.builder()
                        .modelName(completions.model())
                        .tokenUsage(toTokenUsageV2(completions.usage()))
                        .finishReason(finishReasonFromV2(completions.finishReason()))
                        .build())
                .build();
    }

    private static ChatMessageV2 findAssistantMessage(List<ChatMessageV2> messages) {
        return messages.stream()
                .filter(msg -> msg.role() == ChatMessageRoleV2.ASSISTANT)
                .findFirst()
                .orElse(null);
    }

    private static AiMessage convertChatMessageV2ToAiMessage(ChatMessageV2 message) {
        String text = extractTextFromContent(message.content());

        if (hasToolCalls(message.content())) {
            List<ToolExecutionRequest> toolExecutionRequests = extractToolCallsFromContent(message.content(),
                    message.toolsStateId());
            return AiMessage.builder()
                    .text(text)
                    .toolExecutionRequests(toolExecutionRequests)
                    .build();
        }

        return AiMessage.from(text);
    }

    private static String extractTextFromContent(List<MessageContentPartV2> content) {
        if (content == null || content.isEmpty()) {
            return "";
        }

        return content.stream()
                .filter(part -> part.text() != null)
                .map(MessageContentPartV2::text)
                .collect(Collectors.joining("\n"));
    }

    private static boolean hasToolCalls(List<MessageContentPartV2> content) {
        if (content == null) {
            return false;
        }

        return content.stream()
                .anyMatch(part -> part.functionCall() != null);
    }

    private static List<ToolExecutionRequest> extractToolCallsFromContent(List<MessageContentPartV2> content,
            String toolId) {
        if (content == null) {
            return Collections.emptyList();
        }

        return content.stream()
                .filter(part -> part.functionCall() != null)
                .map(part -> {
                    FunctionCallContentV2 functionCall = part.functionCall();
                    String arguments;
                    try {
                        if (functionCall.arguments() != null) {
                            // arguments() should return JsonNode, serialize it properly
                            arguments = JsonUtils.objectMapper().writeValueAsString(functionCall.arguments());
                        } else {
                            arguments = "{}";
                        }
                    } catch (Exception e) {
                        // Fallback to toString() if serialization fails
                        arguments = functionCall.arguments() != null ? functionCall.arguments().toString() : "{}";
                    }
                    return ToolExecutionRequest.builder()
                            .id(toolId)
                            .name(functionCall.name())
                            .arguments(arguments)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private static List<ChatMessageV2> convertChatMessagesV2(
            List<dev.langchain4j.data.message.ChatMessage> messages,
            GigaChatChatRequestParameters parameters) {
        return messages.stream()
                .map(message -> convertMessageV2(message, parameters))
                .collect(Collectors.toList());
    }

    private static ChatMessageV2 convertMessageV2(dev.langchain4j.data.message.ChatMessage message,
            GigaChatChatRequestParameters parameters) {
        if (message instanceof UserMessage userMessage) {
            String text = userMessage.contents().stream()
                    .filter(TextContent.class::isInstance)
                    .map(c -> ((TextContent) c).text())
                    .findFirst()
                    .orElse("");

            List<FileRefV2> files = Optional.ofNullable(parameters)
                    .map(GigaChatChatRequestParameters::getAttachments)
                    .map(attachments -> attachments.stream()
                            .map(id -> FileRefV2.builder()
                                    .id(id)
                                    .build())
                            .toList())
                    .orElse(List.of());

            return ChatMessageV2.builder()
                    .role(ChatMessageRoleV2.USER)
                    .contentPart(MessageContentPartV2.builder()
                            .text(text)
                            .files(files)
                            .build())
                    .build();
        } else if (message instanceof SystemMessage systemMessage) {
            return ChatMessageV2.textMessage(ChatMessageRoleV2.SYSTEM, systemMessage.text());
        } else if (message instanceof AiMessage aiMessage) {
            var toolRequest = aiMessage.toolExecutionRequests().stream()
                    .findFirst();
            List<MessageContentPartV2> content = toolRequest
                    .map(req -> List.of(MessageContentPartV2.builder()
                            .functionCall(FunctionCallContentV2.builder()
                                    .name(req.name())
                                    .arguments(parseArguments(req.arguments()))
                                    .build())
                            .text(aiMessage.text())
                            .build()))
                    .orElse(List.of());

            return ChatMessageV2.builder()
                    .toolsStateId(toolRequest.map(ToolExecutionRequest::id).orElse(null))
                    .role(ChatMessageRoleV2.ASSISTANT)
                    .content(content)
                    .build();

        } else if (message instanceof ToolExecutionResultMessage toolExecutionResultMessage) {
            try {
                ObjectMapper objectMapper = JsonUtils.objectMapper();
                JsonNode resultNode = objectMapper.valueToTree(toolExecutionResultMessage.text());
                return ChatMessageV2.builder()
                        .role(ChatMessageRoleV2.TOOL)
                        .toolsStateId(toolExecutionResultMessage.id())
                        .contentPart(MessageContentPartV2.builder()
                                .functionResult(FunctionResultContentV2.builder()
                                        .name(toolExecutionResultMessage.toolName())
                                        .result(resultNode)
                                        .build())
                                .build())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to convert tool execution result to JSON", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
        }
    }

    private static Map<String, Object> parseArguments(String json) {
        try {
            return JsonUtils.objectMapper().readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse tool arguments", e);
        }
    }

    private static List<ToolV2> convertToolsV2(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null || toolSpecifications.isEmpty()) {
            return Collections.emptyList();
        }

        List<FunctionSpecificationV2> functionSpecs = toolSpecifications.stream()
                .map(GigaChatHelperV2::convertToolSpecificationV2)
                .collect(Collectors.toList());

        FunctionsToolPayloadV2 functionsPayload = FunctionsToolPayloadV2.builder()
                .specifications(functionSpecs)
                .build();

        return Collections.singletonList(ToolV2.ofFunctions(functionsPayload));
    }

    private static FunctionSpecificationV2 convertToolSpecificationV2(ToolSpecification toolSpecification) {
        JsonNode parameters = convertJsonSchemaToJsonNode(toolSpecification.parameters());
        JsonNode returnParameters = extractReturnParameters(toolSpecification);

        return FunctionSpecificationV2.builder()
                .name(toolSpecification.name())
                .description(toolSpecification.description())
                .parameters(parameters)
                .returnParameters(returnParameters)
                .build();
    }

    private static JsonNode convertJsonSchemaToJsonNode(JsonSchemaElement jsonSchema) {
        if (jsonSchema == null) {
            return null;
        }

        ObjectMapper objectMapper = JsonUtils.objectMapper();
        Map<String, Object> schemaMap = toMap(jsonSchema, false);

        try {
            return objectMapper.valueToTree(schemaMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON schema to JsonNode", e);
        }
    }

    private static JsonNode extractReturnParameters(ToolSpecification toolSpecification) {
        try {
            Object returnParams = toolSpecification.metadata().get("return_parameters");
            if (returnParams != null) {
                ObjectMapper objectMapper = JsonUtils.objectMapper();
                return objectMapper.valueToTree(returnParams);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract return_parameters from ToolSpecification metadata", e);
        }
    }

    public static TokenUsage toTokenUsageV2(CompletionStreamUsageV2 usage) {
        if (usage == null) {
            return null;
        }
        return new TokenUsage(
                usage.promptTokens() != null ? usage.promptTokens() : usage.inputTokens(),
                usage.completionTokens() != null ? usage.completionTokens() : usage.outputTokens(),
                usage.totalTokens());
    }

    public static FinishReason finishReasonFromV2(String reason) {
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

    public static chat.giga.model.completion.ResponseFormat toResponseFormatV2(ResponseFormat responseFormat,
            Boolean strict) {
        return chat.giga.langchain4j.utils.GigaChatHelper.toResponseFormat(responseFormat, strict);
    }

    public static Boolean toDisableFilter(Boolean profanityCheck) {
        if (profanityCheck == null) {
            return null;
        }
        return !profanityCheck;
    }
}
