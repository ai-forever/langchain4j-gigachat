package chat.giga.langchain4j;

import chat.giga.client.GigaChatClientAsync;
import chat.giga.client.ResponseHandler;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.ChoiceMessageFunctionCall;
import chat.giga.model.completion.CompletionChunkResponse;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

import static chat.giga.langchain4j.GigaChatHelper.finishReasonFrom;
import static chat.giga.langchain4j.GigaChatHelper.toRequest;
import static chat.giga.langchain4j.GigaChatHelper.toTokenUsage;
import static dev.langchain4j.internal.Utils.getOrDefault;

public class GigaChatStreamingChatModel implements StreamingChatLanguageModel {

    private final GigaChatClientAsync asyncClient;
    private final List<ChatModelListener> listeners;
    private final DefaultChatRequestParameters defaultChatRequestParameters;

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
            DefaultChatRequestParameters defaultChatRequestParameters) {

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
        this.defaultChatRequestParameters = (DefaultChatRequestParameters) getOrDefault(defaultChatRequestParameters,
                DefaultChatRequestParameters.builder().build());
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

            //TODO need to check
            if (choice.delta().functionCall() != null) {
                final ChoiceMessageFunctionCall function = choice.delta().functionCall();
                final String functionName;
                final String functionArguments;
                if (function.name() != null) {
                    functionName = function.name();
                } else {
                    functionName = "";
                }
                if (function.arguments() != null && !function.arguments().isEmpty()) {
                    functionArguments = function.arguments().toString();
                } else {
                    functionArguments = "";
                }

                // TODO is new or existing?
                ToolExecutionRequest toolExecutionRequest = ToolExecutionRequest.builder()
                        .name(functionName)
                        .arguments(functionArguments)
                        .build();
                toolExecutionRequests.add(toolExecutionRequest);
            }
            if (choice.finishReason() != null) {
                responseMetadataBuilder.finishReason(
                        finishReasonFrom(choice.finishReason().value()));
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
