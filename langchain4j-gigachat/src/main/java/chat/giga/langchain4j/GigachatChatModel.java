package chat.giga.langchain4j;


import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
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
import static dev.langchain4j.model.output.FinishReason.CONTENT_FILTER;
import static dev.langchain4j.model.output.FinishReason.LENGTH;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static dev.langchain4j.model.output.FinishReason.TOOL_EXECUTION;

public class GigachatChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final GigaChatClient client;
    private final Tokenizer tokenizer;
    private final Integer maxRetries;
    private final List<ChatModelListener> listeners;

    @Builder
    public GigachatChatModel(GigachatChatModelBuilder builder) {
        this.client = GigaChatClient.builder()
                .apiHttpClient(builder.apiHttpClient)
                .apiUrl(builder.apiUrl)
                .authClient(builder.authClient)
                .connectTimeout(builder.connectTimeout)
                .readTimeout(builder.readTimeout)
                .logRequests(builder.logRequests)
                .logResponses(builder.logResponses)
                .verifySslCerts(builder.verifySslCerts)
                .build();
        this.tokenizer = builder.tokenizer;
        this.maxRetries = builder.maxRetries;
        this.listeners = builder.listeners;
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
                    ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                            .name(s.message().functionCall().name())
                            .arguments(s.message().functionCall().arguments().toString())
                            .build();
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
                        .map(m -> {
                                    return chat.giga.model.completion.ChatMessage.builder()
                                            .content(m.text())
                                            .build();
                                }
                        ).collect(Collectors.toList())
                )
                .temperature(chatRequest.parameters().temperature().floatValue())
                .topP(chatRequest.parameters().topP().floatValue())
                .maxTokens(maxRetries)
                .repetitionPenalty(chatRequest.parameters().frequencyPenalty().floatValue())
                .function(null)
                .build();
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

    public static class GigachatChatModelBuilder {

        private HttpClient apiHttpClient;
        private AuthClient authClient;
        private Integer readTimeout;
        private Integer connectTimeout;
        private String apiUrl;
        private Boolean logRequests;
        private Boolean logResponses;
        private Boolean verifySslCerts;
        private Tokenizer tokenizer;
        private Integer maxRetries;
        private List<ChatModelListener> listeners;

        public GigachatChatModelBuilder apiHttpClient(HttpClient apiHttpClient) {
            this.apiHttpClient = apiHttpClient;
            return this;
        }

        public GigachatChatModelBuilder authClient(AuthClient authClient) {
            this.authClient = authClient;
            return this;
        }

        public GigachatChatModelBuilder readTimeout(Integer readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public GigachatChatModelBuilder connectTimeout(Integer connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public GigachatChatModelBuilder apiUrl(String apiUrl) {
            this.apiUrl = apiUrl;
            return this;
        }

        public GigachatChatModelBuilder logRequests(Boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        public GigachatChatModelBuilder logResponses(Boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        public GigachatChatModelBuilder verifySslCerts(Boolean verifySslCerts) {
            this.verifySslCerts = verifySslCerts;
            return this;
        }

        public GigachatChatModelBuilder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public GigachatChatModelBuilder maxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public GigachatChatModelBuilder listeners(List<ChatModelListener> listeners) {
            this.listeners = listeners;
            return this;
        }
    }
}
