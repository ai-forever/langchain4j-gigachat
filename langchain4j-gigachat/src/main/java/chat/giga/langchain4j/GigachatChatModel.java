package chat.giga.langchain4j;


import chat.giga.client.GigaChatClient;
import chat.giga.model.completion.CompletionRequest;
import chat.giga.model.completion.CompletionResponse;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.TokenCountEstimator;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.Builder;

import java.util.List;

import static java.util.Collections.emptyList;

public class GigachatChatModel implements ChatLanguageModel, TokenCountEstimator {

    private final GigaChatClient gigaChatClient;
    private final Tokenizer tokenizer;
    private final Integer maxRetries;

    private final List<ChatModelListener> listeners;

    @Builder
    public GigachatChatModel(GigaChatClient client, Tokenizer tokenizer, Integer maxRetries, List<ChatModelListener> listeners) {
        this.gigaChatClient = client;
        this.tokenizer = tokenizer;
        this.maxRetries = maxRetries;
        this.listeners = listeners == null ? emptyList() : listeners;

    }

    @Override
    public ChatResponse doChat(ChatRequest chatRequest) {

        return toResponse(gigaChatClient.completions(toRequest(chatRequest)));
    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public int estimateTokenCount(List<ChatMessage> messages) {
        return tokenizer.estimateTokenCountInMessages(messages);
    }

    private ChatResponse toResponse(CompletionResponse completions) {
        return null;
    }

    private CompletionRequest toRequest(ChatRequest chatRequest) {
        return null;
    }
}
