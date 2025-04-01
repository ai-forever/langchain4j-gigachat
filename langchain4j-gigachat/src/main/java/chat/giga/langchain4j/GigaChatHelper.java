package chat.giga.langchain4j;

import chat.giga.model.completion.ChatFunction;
import chat.giga.model.completion.ChatFunctionParameters;
import chat.giga.model.completion.ChatFunctionParametersProperty;
import chat.giga.model.completion.ChatMessage;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static dev.langchain4j.model.output.FinishReason.CONTENT_FILTER;
import static dev.langchain4j.model.output.FinishReason.LENGTH;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION;

public class GigaChatHelper {

    public static List<ChatMessage> mapperChatMessages(List<dev.langchain4j.data.message.ChatMessage> messages) {
        return messages.stream()
                .map(GigaChatHelper::convertMessage)
                .collect(Collectors.toList());
    }

    private static chat.giga.model.completion.ChatMessage convertMessage(dev.langchain4j.data.message.ChatMessage message) {
        if (message instanceof UserMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessage.Role.USER)
                    .content(((UserMessage) message).text())
                    .build();
        } else if (message instanceof SystemMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessage.Role.SYSTEM)
                    .content(((SystemMessage) message).text())
                    .build();
        } else if (message instanceof AiMessage) {
            return chat.giga.model.completion.ChatMessage.builder()
                    .role(ChatMessage.Role.ASSISTANT)
                    .content(((AiMessage) message).text())
                    .build();
        } else {
            throw new IllegalArgumentException("Unsupported message type: " + message.getClass().getName());
        }
    }

    public static Map<String, ChatFunctionParametersProperty> mapperParameters(Map<String, JsonSchemaElement> inputMap) {
        return inputMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> convertToChatFunctionParametersProperty((JsonObjectSchema) entry.getValue())
                ));
    }

    private static ChatFunctionParametersProperty convertToChatFunctionParametersProperty(JsonObjectSchema jsonObjectSchema) {
        return ChatFunctionParametersProperty.builder()
                .properties(mapperParameters(jsonObjectSchema.properties()))
                .description(jsonObjectSchema.description())
                .build();
    }

    public static ChatResponse toResponse(CompletionResponse completions) {
        return completions.choices()
                .stream()
                .map(s -> {
                    ToolExecutionRequest toolExecutionRequest = null;
                    if (s.message().functionCall() != null) {
                        toolExecutionRequest = ToolExecutionRequest.builder()
                                .name(s.message().functionCall().name())
                                .arguments(s.message().functionCall().arguments().toString())
                                .build();
                    }
                    return ChatResponse.builder()
                            .aiMessage(AiMessage.builder()
                                    .text(s.message().content())
                                    .toolExecutionRequests(Collections.singletonList(toolExecutionRequest))
                                    .build())
                            .metadata(ChatResponseMetadata.builder()
                                    .modelName(completions.model())
                                    .tokenUsage(new TokenUsage(
                                            completions.usage().promptTokens(),
                                            completions.usage().completionTokens(),
                                            completions.usage().totalTokens()))
                                    .finishReason(finishReasonFrom(s.finishReason().value()))
                                    .build())
                            .build();
                })
                .findAny()
                .orElseThrow();
    }

    public static CompletionRequest toRequest(ChatRequest chatRequest) {
        return CompletionRequest.builder()
                .model(chatRequest.parameters().modelName())
                .messages(mapperChatMessages(chatRequest.messages()))
                .temperature(chatRequest.parameters().temperature().floatValue())
                .topP(chatRequest.parameters().topP().floatValue())
                .maxTokens(chatRequest.parameters().maxOutputTokens())
                .repetitionPenalty(chatRequest.parameters().frequencyPenalty().floatValue())
                .functions(chatRequest.toolSpecifications()
                        .stream()
                        .map(toolSpecification -> {
                            var chatFunctionParameters = ChatFunctionParameters.builder()
                                    .required(toolSpecification.parameters().required())
                                    .properties(mapperParameters(toolSpecification.parameters().properties()))
                                    .build();
                            return ChatFunction.builder()
                                    .name(toolSpecification.name())
                                    .description(toolSpecification.description())
                                    .parameters(chatFunctionParameters)
                                    .build();
                        })
                        .collect(Collectors.toList())
                )
                .build();
    }

    private static FinishReason finishReasonFrom(String reason) {
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
}
