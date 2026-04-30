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
import static chat.giga.langchain4j.utils.GigaChatHelperV2.shouldUseV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toRequestV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toResponseV2;
import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.copy;
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

    /**
     * Клиент для работы с GigaChat API
     */
    private final GigaChatClient client;

    /** Максимальное количество повторных попыток при ошибках сети */
    private final Integer maxRetries;

    /** Слушатели событий модели */
    private final List<ChatModelListener> listeners;

    /** Поддерживаемые возможности модели */
    private final Set<Capability> supportedCapabilities;

    /** Параметры запроса по умолчанию */
    private final GigaChatChatRequestParameters defaultChatRequestParameters;

    /**
     * Создает экземпляр GigaChatChatModel с использованием builder pattern.
     *
     * @param apiHttpClient HTTP-клиент для API запросов (опционально)
     * @param authClient клиент аутентификации для получения токенов доступа
     * @param readTimeout таймаут чтения в миллисекундах (опционально)
     * @param connectTimeout таймаут подключения в миллисекундах (опционально)
     * @param apiUrl URL API GigaChat (опционально, по умолчанию используется официальный эндпоинт)
     * @param logRequests флаг логирования исходящих запросов
     * @param logResponses флаг логирования входящих ответов
     * @param verifySslCerts флаг проверки SSL сертификатов
     * @param maxRetries максимальное количество повторных попыток при ошибках сети
     * @param maxRetriesOnAuthError максимальное количество повторных попыток при ошибках аутентификации
     * @param listeners слушатели событий модели
     * @param responseFormat формат ответа модели (JSON schema или текст)
     * @param strictJsonSchema флаг строгой валидации JSON схемы
     * @param supportedCapabilities поддерживаемые возможности модели
     * @param defaultChatRequestParameters параметры запроса по умолчанию
     */
    @Builder
    public GigaChatChatModel(HttpClient apiHttpClient,
                             AuthClient authClient,
                             Integer readTimeout,
                             Integer connectTimeout,
                             String apiUrl,
            String apiV2Url,
                             boolean logRequests,
                             boolean logResponses,
                             boolean verifySslCerts,
                             Integer maxRetries,
            Integer maxRetriesOnAuthError,
                             List<ChatModelListener> listeners,
            ResponseFormat responseFormat,
            Boolean strictJsonSchema,
            Set<Capability> supportedCapabilities,
            GigaChatChatRequestParameters defaultChatRequestParameters) {
        this.client = GigaChatClient.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .apiV2Url(apiV2Url)
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
        ChatRequestParameters commonParameters = defaultChatRequestParameters != null
                ? defaultChatRequestParameters
                : DefaultChatRequestParameters.builder().build();
        GigaChatChatRequestParameters gigaChatParameters = defaultChatRequestParameters != null
                ? defaultChatRequestParameters
                : GigaChatChatRequestParameters.builder().build();

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
                .useV2Completions(getOrDefault(gigaChatParameters.getUseV2Completions(), false))
                .build();
    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {
        boolean useV2 = shouldUseV2(chatRequest);
        String sessionId = chatRequest.parameters() instanceof GigaChatChatRequestParameters gigaChatParameters
                ? gigaChatParameters.getSessionId()
                : defaultChatRequestParameters.getSessionId();
        if (useV2) {
            CompletionRequestV2 requestV2 = toRequestV2(chatRequest);
            CompletionResponseV2 responseV2 = withRetry(() ->
                            client.completionsV2(requestV2, sessionId),
                    maxRetries);
            return toResponseV2(responseV2);
        } else {
            return toResponse(withRetry(
                    () -> client.completions(toRequest(chatRequest), sessionId),
                    maxRetries));
        }
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
