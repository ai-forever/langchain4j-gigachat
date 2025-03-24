package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.model.embedding.EmbeddingRequest;
import chat.giga.model.embedding.EmbeddingResponse;
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
    public GigaChatEmbeddingModel(GigaChatClient client, String modelName, Integer batchSize) {
        this.gigaChatClient = client;
        this.modelName = modelName;
        this.batchSize = getOrDefault(batchSize, 16);
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = textSegments.stream().map(TextSegment::text).collect(toList());
        return embedTexts(texts);
    }


    private Response<List<Embedding>> embedTexts(List<String> texts) {
        List<Embedding> embeddings = new ArrayList<>();
        int inputTokenCount = 0;

        for (int i = 0; i < texts.size(); i += batchSize) {
            List<String> batch = texts.subList(i, Math.min(i + batchSize, texts.size()));
            EmbeddingRequest request = EmbeddingRequest.builder().input(batch).model(modelName).build();
            EmbeddingResponse response = gigaChatClient.embeddings(request);

            for (chat.giga.model.embedding.Embedding embeddingItem : response.data()) {
                Embedding embedding = Embedding.from(embeddingItem.embedding());
                embeddings.add(embedding);
                inputTokenCount += embeddingItem.usage().promptTokens();
            }
        }

        return Response.from(embeddings, new TokenUsage(inputTokenCount));
    }
}
