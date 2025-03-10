package ru.sber.sbe.gigachat;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import lombok.Builder;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.*;
import ru.sber.sbe.gigachat.internal.client.GigachatClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static java.util.stream.Collectors.toList;

/**
 * @author protas-nv 13.01.2025
 */
public class GigachatEmbedModel implements EmbeddingModel {

    private static final int BATCH_SIZE = 16;
    private final GigachatClient client;

    @Builder
    public GigachatEmbedModel(String baseUrl, Duration timeout, Boolean logRequests,
                              Boolean logResponses, String caBundleFile, String keyFilePath, String certFilePath) {
        this.client = GigachatClient.builder()
                                    .baseUrl(ensureNotBlank(baseUrl, "baseUrl cannot be empty."))
                                    .timeout(getOrDefault(timeout, Duration.ofSeconds(60)))
                                    .logRequests(getOrDefault(logRequests, false))
                                    .logResponses(getOrDefault(logResponses, false))
                                    .caBundleFile(caBundleFile)
                                    .keyFilePath(keyFilePath)
                                    .certFilePath(certFilePath)
                                    .build();
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> textSegments) {
        List<String> texts = textSegments.stream().map(TextSegment::text).collect(toList());
        return embedTexts(texts);
    }

    private Response<List<Embedding>> embedTexts(List<String> texts) {
        List<Embedding> embeddings = new ArrayList<>();
        int inputTokenCount = 0;

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH_SIZE, texts.size()));
            GigaEmbedRequest request = GigaEmbedRequest.builder().input(batch).build();
            GigaEmbedResponse response = client.createEmbedding(request);

            for (EmbeddingDataInner embeddingItem : response.getData()) {
                Embedding embedding = Embedding.from(embeddingItem.getEmbedding());
                embeddings.add(embedding);
                inputTokenCount += embeddingItem.getUsage().getPromptTokens();
            }
        }

        return Response.from(embeddings, new TokenUsage(inputTokenCount));
    }
}