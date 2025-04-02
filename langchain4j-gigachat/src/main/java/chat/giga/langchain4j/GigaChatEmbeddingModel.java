package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
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

public class GigaChatEmbeddingModel extends DimensionAwareEmbeddingModel {

    private final GigaChatClient gigaChatClient;
    private final String modelName;
    private final Integer batchSize;

    @Builder
    public GigaChatEmbeddingModel(HttpClient apiHttpClient,
                                  AuthClient authClient,
                                  Integer readTimeout,
                                  Integer connectTimeout,
                                  String apiUrl,
                                  boolean logRequests,
                                  boolean logResponses,
                                  boolean verifySslCerts,
                                  String modelName, Integer batchSize) {
        this.gigaChatClient = GigaChatClient.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .build();
        this.modelName = getOrDefault(modelName, "Embeddings");
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
