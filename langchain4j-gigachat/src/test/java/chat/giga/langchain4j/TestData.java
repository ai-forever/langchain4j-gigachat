package chat.giga.langchain4j;

import chat.giga.model.completion.Choice;
import chat.giga.model.completion.ChoiceChunk;
import chat.giga.model.completion.ChoiceFinishReason;
import chat.giga.model.completion.ChoiceMessage;
import chat.giga.model.completion.ChoiceMessageChunk;
import chat.giga.model.completion.ChoiceMessageFunctionCall;
import chat.giga.model.completion.CompletionChunkResponse;
import chat.giga.model.completion.CompletionResponse;
import chat.giga.model.completion.Usage;
import chat.giga.model.embedding.Embedding;
import chat.giga.model.embedding.EmbeddingResponse;
import chat.giga.model.embedding.EmbeddingUsage;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.request.ChatRequest;

import java.util.Collections;
import java.util.List;

import static chat.giga.model.completion.ChatMessageRole.ASSISTANT;

public class TestData {

    private static final String MESSAGE = """
            Квадратный корень из числа 25 можно найти следующим образом:
            $\\sqrt{25} = ?$
            Мы знаем, что число 25 является квадратом целого числа 5, так как $5^2 = 25$. Следовательно, $\\sqrt{25} = 5$.""";

    public static CompletionChunkResponse completionChunkResponse() {
        return CompletionChunkResponse.builder()
                .choice(ChoiceChunk.builder()
                        .delta(ChoiceMessageChunk.builder()
                                .role(ASSISTANT)
                                .content("test")
                                .functionCall(ChoiceMessageFunctionCall.builder()
                                        .name("testFunc")
                                        .argument("testArg", "testVal")
                                        .build())
                                .build())
                        .index(0)
                        .finishReason(ChoiceFinishReason.STOP)
                        .build())
                .created(3214)
                .model("testModel")
                .object("test")
                .build();
    }

    public static CompletionChunkResponse completionChunkNullFieldsResponse() {
        return CompletionChunkResponse.builder()
                .choice(ChoiceChunk.builder()
                        .delta(ChoiceMessageChunk.builder()
                                .role(ASSISTANT)
                                .content("test")
                                .build())
                        .index(0)
                        .finishReason(ChoiceFinishReason.STOP)
                        .build())
                .created(3214)
                .model("testModel")
                .object("test")
                .build();
    }

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
                                .role(ASSISTANT)
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

    public static CompletionResponse completionNullFinishReasonResponse() {
        return CompletionResponse.builder()
                .choice(Choice.builder()
                        .message(ChoiceMessage.builder()
                                .role(ASSISTANT)
                                .content("No finish reason")
                                .created(12344343)
                                .build())
                        .index(0)
                        .build())
                .usage(Usage.builder()
                        .promptTokens(1)
                        .completionTokens(2)
                        .totalTokens(3)
                        .build())
                .build();
    }

    public static CompletionResponse completionFunctionCallResponse() {
        return CompletionResponse.builder()
                .choice(Choice.builder()
                        .message(ChoiceMessage.builder()
                                .role(ASSISTANT)
                                .content(MESSAGE)
                                .functionCall(ChoiceMessageFunctionCall.builder()
                                        .argument("key", "value")
                                        .name("testFunction")
                                        .build())
                                .functionsStateId("841b498c-9ef1-4791-a329-e86c44727327")
                                .created(12344343)
                                .build())
                        .index(0)
                        .finishReason(ChoiceFinishReason.FUNCTION_CALL)
                        .build())
                .created(321334)
                .model("testModel")
                .usage(Usage.builder()
                        .promptTokens(1)
                        .completionTokens(2)
                        .totalTokens(3)
                        .build())
                .object("test")
                .build();
    }

    public static ChatRequest.Builder chatRequest() {
        return ChatRequest.builder()
                .messages(AiMessage.builder()
                        .text("test message")
                        .toolExecutionRequests(Collections.singletonList(ToolExecutionRequest.builder().build()))
                        .build())
                .parameters(GigaChatChatRequestParameters.builder()
                        .modelName("testModel")
                        .temperature(0.7)
                        .topP(0.9)
                        .maxOutputTokens(100)
                        .repetitionPenalty(1.2f)
                        .updateInterval(500)
                        .functionCall("auto")
                        .build());
    }
}
