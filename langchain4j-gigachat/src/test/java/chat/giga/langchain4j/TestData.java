package chat.giga.langchain4j;

import chat.giga.model.completion.Choice;
import chat.giga.model.completion.ChoiceFinishReason;
import chat.giga.model.completion.ChoiceMessage;
import chat.giga.model.completion.CompletionResponse;
import chat.giga.model.completion.MessageRole;
import chat.giga.model.completion.Usage;
import chat.giga.model.embedding.Embedding;
import chat.giga.model.embedding.EmbeddingResponse;
import chat.giga.model.embedding.EmbeddingUsage;

import java.util.List;

public class TestData {

    public static EmbeddingResponse embeddingResponse() {
        return EmbeddingResponse.builder()
                .model("Embeddings")
                .object("list")
                .data(List.of(Embedding.builder()
                        .usage(EmbeddingUsage.builder()
                                .promptTokens(11)
                                .build())
                        .object("embedding")
                        .embedding(List.of(11f, 22f))
                        .index(0)
                        .build()))
                .build();
    }

    public static CompletionResponse completionResponse() {
        return CompletionResponse.builder()
                .choice(Choice.builder()
                        .message(ChoiceMessage.builder()
                                .role(MessageRole.ASSISTANT)
                                .content(
                                        "<img src=\\\"0987e150-8f95-4b09-b699-d7c5bc14b67f\\\" fuse=\\\"true\\\"/> получился такой милый розовый кот.")
                                .created(1234)
                                .build())
                        .index(0)
                        .finishReason(ChoiceFinishReason.STOP)
                        .build())
                .created(3214)
                .model("testModel")
                .usage(Usage.builder()
                        .promptTokens(1)
                        .completionTokens(2)
                        .totalTokens(3)
                        .build())
                .object("test")
                .build();
    }
}
