package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Builder;

import java.util.List;

import static chat.giga.langchain4j.utils.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.utils.GigaChatHelper.toResponse;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;

public class GigaChatChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final GigaChatClient client;
    private final Tokenizer tokenizer;
    private final Integer maxRetries;
    private final List<ChatModelListener> listeners;
    private final DefaultChatRequestParameters defaultChatRequestParameters;

    @Builder
    public GigaChatChatModel(HttpClient apiHttpClient,
                             AuthClient authClient,
                             Integer readTimeout,
                             Integer connectTimeout,
                             String apiUrl,
                             boolean logRequests,
                             boolean logResponses,
                             boolean verifySslCerts,
                             Tokenizer tokenizer,
                             Integer maxRetries,
                             List<ChatModelListener> listeners,
                             DefaultChatRequestParameters defaultChatRequestParameters) {
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
        this.defaultChatRequestParameters = defaultChatRequestParameters;
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
    public int estimateTokenCount(List<dev.langchain4j.data.message.ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    @Override
    public DefaultChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }
}
