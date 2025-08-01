package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Builder;

import java.util.List;

import static chat.giga.langchain4j.utils.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.utils.GigaChatHelper.toResponse;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;


/**
 * Представляет собой реализацию GigaChat языковой модели интерфейса
 * <p>
 * Описание параметров можно найти  <a
 * href="https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/post-chat">тут</a>
 */
public class GigaChatChatModel implements ChatModel {

    private final GigaChatClient client;
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
                .functionCall(gigaChatParameters.getFunctionCall())
                .attachments(gigaChatParameters.getAttachments())
                .repetitionPenalty(gigaChatParameters.getRepetitionPenalty())
                .sessionId(gigaChatParameters.getSessionId())
                .build();
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        return toResponse(withRetry(() -> client.completions(toRequest(chatRequest), defaultChatRequestParameters.getSessionId()), maxRetries));
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }


    @Override
    public GigaChatChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }
}
