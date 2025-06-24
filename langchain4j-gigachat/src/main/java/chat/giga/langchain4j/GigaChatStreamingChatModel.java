package chat.giga.langchain4j;

import chat.giga.client.GigaChatClientAsync;
import chat.giga.client.ResponseHandler;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.ChoiceFinishReason;
import chat.giga.model.completion.CompletionChunkResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

import static chat.giga.langchain4j.utils.GigaChatHelper.finishReasonFrom;
import static chat.giga.langchain4j.utils.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.utils.GigaChatHelper.toTokenUsage;
import static chat.giga.langchain4j.utils.GigaChatHelper.toToolExecutionRequest;
import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * Представляет собой реализацию GigaChat языковой модели интерфейса. Модель отвечает стримом токен за токеном, а
 * результат должен быть обработан с помощью {@link StreamingChatResponseHandler}.
 * <p> Описание параметров можно найти  <a href="https://developers.sber.ru/docs/ru/gigachat/guides/response-token-streaming">тут</a>
 */
public class GigaChatStreamingChatModel implements StreamingChatModel {

    private final GigaChatClientAsync asyncClient;
    private final List<ChatModelListener> listeners;
    private final GigaChatChatRequestParameters defaultChatRequestParameters;

    @Builder
    public GigaChatStreamingChatModel(HttpClient apiHttpClient,
            AuthClient authClient,
            Integer readTimeout,
            Integer connectTimeout,
            String apiUrl,
            boolean logRequests,
            boolean logResponses,
            boolean verifySslCerts,
            List<ChatModelListener> listeners,
            GigaChatChatRequestParameters defaultChatRequestParameters) {

        this.asyncClient = GigaChatClientAsync.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .build();
        this.listeners = listeners;
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
                .responseFormat(commonParameters.responseFormat())

                // custom
                .updateInterval(getOrDefault(gigaChatParameters.getUpdateInterval(), 0))
                .stream(true)
                .profanityCheck(getOrDefault(gigaChatParameters.getProfanityCheck(), false))
                .functionCall(gigaChatParameters.getFunctionCall())
                .attachments(gigaChatParameters.getAttachments())
                .repetitionPenalty(gigaChatParameters.getRepetitionPenalty())
                .build();
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {

        var responseMetadataBuilder = ChatResponseMetadata.builder();
        var text = new StringBuffer();
        var toolExecutionRequests = new ArrayList<ToolExecutionRequest>();

        try {
            asyncClient.completions(toRequest(chatRequest),
                    new ResponseHandler<>() {
                        @Override
                        public void onNext(CompletionChunkResponse completionChunkResponse) {
                            handlePartialResponse(completionChunkResponse, handler, responseMetadataBuilder, text,
                                    toolExecutionRequests);
                        }

                        @Override
                        public void onComplete() {
                            AiMessage aiMessage;
                            if (!text.toString().isEmpty()) {
                                if (!toolExecutionRequests.isEmpty()) {
                                    aiMessage = AiMessage.from(text.toString(), toolExecutionRequests);
                                } else {
                                    aiMessage = AiMessage.from(text.toString());
                                }
                            } else {
                                if (!toolExecutionRequests.isEmpty()) {
                                    aiMessage = AiMessage.from(toolExecutionRequests);
                                } else {
                                    throw new IllegalArgumentException(
                                            "No text or toolExecutionRequests found in the response");
                                }
                            }
                            var chatResponse = ChatResponse.builder()
                                    .aiMessage(aiMessage)
                                    .metadata(responseMetadataBuilder.build())
                                    .build();
                            handler.onCompleteResponse(chatResponse);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            handler.onError(throwable);
                        }
                    });
        } catch (Exception ex) {
            handler.onError(ex);
        }
    }

    private void handlePartialResponse(CompletionChunkResponse chatCompletionChunk,
            StreamingChatResponseHandler handler,
            ChatResponseMetadata.Builder responseMetadataBuilder, StringBuffer text,
            List<ToolExecutionRequest> toolExecutionRequests) {

        responseMetadataBuilder.modelName(chatCompletionChunk.model());

        chatCompletionChunk.choices().forEach(choice -> {
            if (choice.delta().content() != null) {
                text.append(choice.delta().content());
                handler.onPartialResponse(choice.delta().content());
            }
            if (choice.finishReason() == ChoiceFinishReason.FUNCTION_CALL) {
                toolExecutionRequests.add(toToolExecutionRequest(choice));
            }
            if (choice.finishReason() != null) {
                responseMetadataBuilder.finishReason(finishReasonFrom(choice.finishReason().value()));
            }
        });

        if (chatCompletionChunk.usage() != null) {
            responseMetadataBuilder.tokenUsage(toTokenUsage(chatCompletionChunk.usage()));
        }

    }

    @Override
    public List<ChatModelListener> listeners() {
        return listeners;
    }

    @Override
    public DefaultChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }
}
