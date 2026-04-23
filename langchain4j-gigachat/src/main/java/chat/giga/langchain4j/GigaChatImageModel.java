package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;

import java.util.Base64;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * Представляет собой реализацию GigaChat языковой модели интерфейса для генерации картинок
 * <p> Описание параметров можно найти  <a href="https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/post-chat">тут</a>
 */
public class GigaChatImageModel implements ImageModel {

    /**
     * Клиент для работы с GigaChat API
     */
    private final GigaChatClient client;

    /** Максимальное количество повторных попыток при ошибках сети */
    private final Integer maxRetries;

    /** Чат-модель для обработки запросов на генерацию изображений */
    private final GigaChatChatModel chatModel;

    /**
     * Создает экземпляр GigaChatImageModel с использованием builder pattern.
     *
     * @param apiHttpClient HTTP-клиент для API запросов (опционально)
     * @param authClient клиент аутентификации для получения токенов доступа
     * @param readTimeout таймаут чтения в миллисекундах (опционально)
     * @param connectTimeout таймаут подключения в миллисекундах (опционально)
     * @param apiUrl URL API GigaChat (опционально, по умолчанию используется официальный эндпоинт)
     * @param logRequests флаг логирования исходящих запросов
     * @param logResponses флаг логирования входящих ответов
     * @param verifySslCerts флаг проверки SSL сертификатов
     * @param maxRetries максимальное количество повторных попыток при ошибках сети (опционально)
     * @param maxRetriesOnAuthError максимальное количество повторных попыток при ошибках аутентификации (опционально)
     * @param listeners слушатели событий модели (опционально)
     * @param defaultChatRequestParameters параметры запроса по умолчанию (опционально)
     */
    @Builder
    public GigaChatImageModel(HttpClient apiHttpClient,
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
            GigaChatChatRequestParameters defaultChatRequestParameters) {
        chatModel = GigaChatChatModel.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .listeners(listeners)
                .defaultChatRequestParameters(defaultChatRequestParameters)
                .maxRetries(maxRetries)
                .maxRetriesOnAuthError(maxRetriesOnAuthError)
                .build();

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
    }


    @Override
    public Response<Image> generate(String userMessage) {
        var response = chatModel.chat(ChatRequest.builder()
                .messages(UserMessage.from(userMessage))
                .build());

        if (response != null && response.aiMessage() != null && response.aiMessage().text() != null) {
            var completionsResponse = response.aiMessage().text();
            if (completionsResponse.contains("img src=")) {

                var fileId = completionsResponse.split("\"")[1];

                var file = withRetry(() -> client.downloadFile(fileId, null), maxRetries);

                var base64FileData = Base64.getEncoder().encodeToString(file);

                return Response.from(Image.builder().base64Data(base64FileData).build(), response.tokenUsage());
            } else {
                throw new RuntimeException("No image was generated response does not contain 'img src='");
            }
        } else {
            throw new RuntimeException("No image was generated response is null");
        }
    }
}
