package chat.giga.langchain4j;


import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.output.FinishReason.*;

public class GigachatChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final GigaChatClient client;
    private final Tokenizer tokenizer;
    private final Integer maxRetries;
    private final List<ChatModelListener> listeners;

    @Builder
    public GigachatChatModel(HttpClient apiHttpClient,
                             AuthClient authClient,
                             Integer readTimeout,
                             Integer connectTimeout,
                             String apiUrl,
                             boolean logRequests,
                             boolean logResponses,
                             boolean verifySslCerts,
                             Tokenizer tokenizer,
                             Integer maxRetries,
                             List<ChatModelListener> listeners) {
        this.client = GigaChatClient.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .build();
        this.tokenizer = tokenizer;
        this.maxRetries = getOrDefault(maxRetries, 1);
        this.listeners = listeners;
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        return toResponse(withRetry(() -> client.completions(toRequest(chatRequest)), maxRetries));
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    private ChatResponse toResponse(CompletionResponse completions) {
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

    private CompletionRequest toRequest(ChatRequest chatRequest) {
        return CompletionRequest.builder()
                .model(chatRequest.parameters().modelName())
                .messages(chatRequest.messages().stream()
                        .map(m -> chat.giga.model.completion.ChatMessage.builder()
                                .role(mapRole(m.type()))
                                .content(m.text())
                                .build()
                        ).collect(Collectors.toList())
                )
                .temperature(chatRequest.parameters().temperature() != null ? chatRequest.parameters().temperature().floatValue() : null)
                .topP(chatRequest.parameters().topP() != null ? chatRequest.parameters().topP().floatValue() : null)
                .maxTokens(chatRequest.parameters().maxOutputTokens())
                .repetitionPenalty(chatRequest.parameters().frequencyPenalty() != null ? chatRequest.parameters().frequencyPenalty().floatValue() : null)
                //.function(null)
                .build();
    }

    private chat.giga.model.completion.ChatMessage.Role mapRole(ChatMessageType type) {
        return switch (type) {
            case SYSTEM -> chat.giga.model.completion.ChatMessage.Role.SYSTEM;
            case USER -> chat.giga.model.completion.ChatMessage.Role.USER;
            case TOOL_EXECUTION_RESULT -> chat.giga.model.completion.ChatMessage.Role.FUNCTION;
            case AI -> chat.giga.model.completion.ChatMessage.Role.ASSISTANT;
            case CUSTOM -> chat.giga.model.completion.ChatMessage.Role.USER;
        };
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
}
