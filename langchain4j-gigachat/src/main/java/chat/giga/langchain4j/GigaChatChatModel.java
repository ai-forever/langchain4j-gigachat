package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.ChatFunctionCallEnum;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Builder;

import java.util.List;
import java.util.Objects;

import static chat.giga.langchain4j.utils.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.utils.GigaChatHelper.toResponse;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;

public class GigaChatChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final GigaChatClient client;
    private final Tokenizer tokenizer;
    private final Integer maxRetries;
    private final List<ChatModelListener> listeners;
    private final GigaChatChatRequestParameters defaultChatRequestParameters;

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
                             GigaChatChatRequestParameters defaultChatRequestParameters) {
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
        ChatRequestParameters commonParameters;
        if (defaultChatRequestParameters != null) {
            commonParameters = defaultChatRequestParameters;
        } else {
            commonParameters = DefaultChatRequestParameters.builder().build();
        }
        GigaChatChatRequestParameters gigaChatParameters;
        if (defaultChatRequestParameters != null) {
            gigaChatParameters = defaultChatRequestParameters;
        } else {
            gigaChatParameters = GigaChatChatRequestParameters.builder().build();
        }

        Objects.requireNonNull(commonParameters.modelName(), "Model name must not be null");

        this.defaultChatRequestParameters = GigaChatChatRequestParameters.builder()
                // default
                .modelName(commonParameters.modelName())
                .temperature(commonParameters.temperature())
                .topP(commonParameters.topP())
                .frequencyPenalty(commonParameters.frequencyPenalty())
                .presencePenalty(commonParameters.presencePenalty())
                .maxOutputTokens(commonParameters.maxOutputTokens())
                .stopSequences(commonParameters.stopSequences())
                .toolSpecifications(commonParameters.toolSpecifications())
                .toolChoice(commonParameters.toolChoice())
                .responseFormat(commonParameters.responseFormat())

                // custom
                .updateInterval(getOrDefault(gigaChatParameters.getUpdateInterval(), 0))
                .stream(false)
                .profanityCheck(getOrDefault(gigaChatParameters.getProfanityCheck(), false))
                .functionCall(getOrDefault(gigaChatParameters.getFunctionCall(), ChatFunctionCallEnum.AUTO))
                .attachments(gigaChatParameters.getAttachments())
                .repetitionPenalty(gigaChatParameters.getRepetitionPenalty())
                .build();
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
    public GigaChatChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }
}
