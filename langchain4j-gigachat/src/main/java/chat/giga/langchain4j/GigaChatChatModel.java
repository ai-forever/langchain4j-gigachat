package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.CompletionResponseV2;
import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.ResponseFormatType;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Builder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static chat.giga.langchain4j.utils.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.utils.GigaChatHelper.toResponse;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.copy;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toRequestV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toResponseV2;
import static dev.langchain4j.internal.Utils.firstNotNull;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.model.chat.Capability.RESPONSE_FORMAT_JSON_SCHEMA;


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
    private final Set<Capability> supportedCapabilities;
    private final GigaChatChatRequestParameters defaultChatRequestParameters;
    private final Boolean useV2Completions;

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
            Integer maxRetriesOnAuthError,
                             List<ChatModelListener> listeners,
            ResponseFormat responseFormat,
            Boolean strictJsonSchema,
            Set<Capability> supportedCapabilities,
            GigaChatChatRequestParameters defaultChatRequestParameters,
            Boolean useV2Completions) {
        this.client = GigaChatClient.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .maxRetriesOnAuthError(maxRetriesOnAuthError)
                .build();
        this.maxRetries = getOrDefault(maxRetries, 1);
        this.listeners = copy(listeners);
        this.supportedCapabilities = copy(supportedCapabilities);
        this.useV2Completions = getOrDefault(useV2Completions, false);
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

                // custom
                .updateInterval(getOrDefault(gigaChatParameters.getUpdateInterval(), 0))
                .stream(false)
                .profanityCheck(getOrDefault(gigaChatParameters.getProfanityCheck(), false))
                .functionCall(gigaChatParameters.getFunctionCall())
                .attachments(gigaChatParameters.getAttachments())
                .repetitionPenalty(gigaChatParameters.getRepetitionPenalty())
                .sessionId(gigaChatParameters.getSessionId())
                .strictJsonSchema(
                        firstNotNull("strictJsonSchema", strictJsonSchema, gigaChatParameters.getStrictJsonSchema(),
                                false))
                .responseFormat(getOrDefault(responseFormat, commonParameters.responseFormat()))
                .useV2Completions(
                        firstNotNull("useV2Completions", useV2Completions, gigaChatParameters.getUseV2Completions(),
                                false))
                .build();
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        boolean useV2 = shouldUseV2(chatRequest);
        if (useV2) {
            CompletionRequestV2 requestV2 = toRequestV2(chatRequest, defaultChatRequestParameters);
            CompletionResponseV2 responseV2 = withRetry(() ->
                            client.completionsV2(requestV2, defaultChatRequestParameters.getSessionId()),
                    maxRetries);
            return toResponseV2(responseV2);
        } else {
            return toResponse(withRetry(
                    () -> client.completions(toRequest(chatRequest), defaultChatRequestParameters.getSessionId()),
                    maxRetries));
        }
    }

    private boolean shouldUseV2(ChatRequest chatRequest) {
        Boolean requestUseV2 = null;
        if (chatRequest.parameters() instanceof GigaChatChatRequestParameters gigaChatParameters) {
            requestUseV2 = gigaChatParameters.getUseV2Completions();
        }
        return getOrDefault(requestUseV2, useV2Completions);
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public Set<Capability> supportedCapabilities() {
        Set<Capability> capabilities = new HashSet<>(supportedCapabilities);
        ResponseFormat responseFormat = this.defaultChatRequestParameters.responseFormat();
        if (responseFormat != null && ResponseFormatType.JSON.equals(responseFormat.type())) {
            capabilities.add(RESPONSE_FORMAT_JSON_SCHEMA);
        }
        return capabilities;
    }

    @Override
    public GigaChatChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }
}
