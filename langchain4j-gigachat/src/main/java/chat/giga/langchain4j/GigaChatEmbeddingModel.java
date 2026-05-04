package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.ModelName;
import chat.giga.model.embedding.EmbeddingRequest;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;

import static java.util.stream.Collectors.toList;

/**
 * Представляет собой реализацию Embedding модели GigaChat
 */
public class GigaChatEmbeddingModel extends DimensionAwareEmbeddingModel {

    /**
     * Клиент для работы с GigaChat API
     */
    private final GigaChatClient gigaChatClient;

    /** Имя модели для генерации эмбеддингов */
    private final String modelName;

    /** Размер батча для обработки текстов */
    private final Integer batchSize;

    /**
     * Создает экземпляр GigaChatEmbeddingModel с использованием builder pattern.
     *
     * @param apiHttpClient HTTP-клиент для API запросов (опционально)
     * @param authClient клиент аутентификации для получения токенов доступа
     * @param readTimeout таймаут чтения в миллисекундах (опционально)
     * @param connectTimeout таймаут подключения в миллисекундах (опционально)
     * @param apiUrl URL API GigaChat (опционально, по умолчанию используется официальный эндпоинт)
     * @param logRequests флаг логирования исходящих запросов
     * @param logResponses флаг логирования входящих ответов
     * @param verifySslCerts флаг проверки SSL сертификатов
     * @param modelName имя модели для генерации эмбеддингов (опционально)
     * @param batchSize размер батча для обработки текстов (опционально, по умолчанию 16)
     * @param maxRetriesOnAuthError максимальное количество повторных попыток при ошибках аутентификации (опционально)
     */
    @Builder
    public GigaChatEmbeddingModel(HttpClient apiHttpClient,
                                  AuthClient authClient,
                                  Integer readTimeout,
                                  Integer connectTimeout,
                                  String apiUrl,
                                  boolean logRequests,
                                  boolean logResponses,
                                  boolean verifySslCerts,
            String modelName, Integer batchSize,
            Integer maxRetriesOnAuthError) {
        this.gigaChatClient = GigaChatClient.builder()
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
        this.modelName = getOrDefault(modelName, ModelName.EMBEDDINGS);
        this.batchSize = getOrDefault(batchSize, 16);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        var texts = textSegments.stream()
                .map(TextSegment::text)
                .collect(toList());
        return embedTexts(texts);
    }


    private Response<List<Embedding>> embedTexts(List<String> texts) {
        var embeddings = new ArrayList<Embedding>();
        int inputTokenCount = 0;

        for (int i = 0; i < texts.size(); i += batchSize) {
            var batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            var request = EmbeddingRequest.builder()
                    .input(batch).model(modelName)
                    .build();

            var response = gigaChatClient.embeddings(request);

            for (var embeddingItem : response.data()) {
                var embedding = Embedding.from(embeddingItem.embedding());
                embeddings.add(embedding);
                inputTokenCount += embeddingItem.usage().promptTokens();
            }
        }

        return Response.from(embeddings, new TokenUsage(inputTokenCount));
    }
}
