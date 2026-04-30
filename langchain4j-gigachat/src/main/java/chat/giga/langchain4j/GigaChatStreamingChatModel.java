package chat.giga.langchain4j;

import chat.giga.client.CompletionV2StreamHandler;
import chat.giga.client.GigaChatClientAsync;
import chat.giga.client.ResponseHandler;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.model.completion.ChoiceFinishReason;
import chat.giga.model.completion.CompletionChunkResponse;
import chat.giga.model.v2.completion.CompletionRequestV2;
import chat.giga.model.v2.completion.stream.CompletionMessageDeltaEventV2;
import chat.giga.model.v2.completion.stream.CompletionMessageDoneEventV2;
import chat.giga.model.v2.completion.stream.CompletionToolLifecycleEventV2;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.ResponseFormat;
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
import static chat.giga.langchain4j.utils.GigaChatHelperV2.finishReasonFromV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.shouldUseV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toRequestV2;
import static chat.giga.langchain4j.utils.GigaChatHelperV2.toTokenUsageV2;
import static dev.langchain4j.internal.Utils.copy;
import static dev.langchain4j.internal.Utils.firstNotNull;
import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * Представляет собой реализацию GigaChat языковой модели интерфейса. Модель отвечает стримом токен за токеном, а
 * результат должен быть обработан с помощью {@link StreamingChatResponseHandler}.
 * <p> Описание параметров можно найти  <a href="https://developers.sber.ru/docs/ru/gigachat/guides/response-token-streaming">тут</a>
 */
public class GigaChatStreamingChatModel implements StreamingChatModel {

    /**
     * Асинхронный клиент для работы с GigaChat API в потоковом режиме
     */
    private final GigaChatClientAsync asyncClient;

    /** Слушатели событий модели */
    private final List<ChatModelListener> listeners;

    /** Параметры запроса по умолчанию */
    private final GigaChatChatRequestParameters defaultChatRequestParameters;

    /**
     * Создает экземпляр GigaChatStreamingChatModel с использованием builder pattern.
     *
     * @param apiHttpClient HTTP-клиент для API запросов (опционально)
     * @param authClient клиент аутентификации для получения токенов доступа
     * @param readTimeout таймаут чтения в миллисекундах (опционально)
     * @param connectTimeout таймаут подключения в миллисекундах (опционально)
     * @param apiUrl URL API GigaChat (опционально, по умолчанию используется официальный эндпоинт)
     * @param logRequests флаг логирования исходящих запросов
     * @param logResponses флаг логирования входящих ответов
     * @param verifySslCerts флаг проверки SSL сертификатов
     * @param listeners слушатели событий модели
     * @param responseFormat формат ответа модели (JSON schema или текст) (опционально)
     * @param strictJsonSchema флаг строгой валидации JSON схемы (опционально)
     * @param maxRetriesOnAuthError максимальное количество повторных попыток при ошибках аутентификации (опционально)
     * @param defaultChatRequestParameters параметры запроса по умолчанию (опционально)
     */
    @Builder
    public GigaChatStreamingChatModel(HttpClient apiHttpClient,
            AuthClient authClient,
            Integer readTimeout,
            Integer connectTimeout,
            String apiUrl,
            String apiV2Url,
            boolean logRequests,
            boolean logResponses,
            boolean verifySslCerts,
            List<ChatModelListener> listeners,
            ResponseFormat responseFormat,
            Boolean strictJsonSchema,
            Integer maxRetriesOnAuthError,
            GigaChatChatRequestParameters defaultChatRequestParameters) {

        this.asyncClient = GigaChatClientAsync.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .apiV2Url(apiV2Url)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .maxRetriesOnAuthError(maxRetriesOnAuthError)
                .build();
        this.listeners = copy(listeners);
        ChatRequestParameters commonParameters = defaultChatRequestParameters != null
                ? defaultChatRequestParameters
                : DefaultChatRequestParameters.builder().build();
        GigaChatChatRequestParameters gigaChatParameters = defaultChatRequestParameters != null
                ? defaultChatRequestParameters
                : GigaChatChatRequestParameters.builder().build();
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
                .strictJsonSchema(
                        firstNotNull("strictJsonSchema", strictJsonSchema, gigaChatParameters.getStrictJsonSchema(),
                                false))
                .responseFormat(getOrDefault(responseFormat, commonParameters.responseFormat()))
                .useV2Completions(getOrDefault(gigaChatParameters.getUseV2Completions(), false))
                .build();
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        boolean useV2 = shouldUseV2(chatRequest);
        String sessionId = chatRequest.parameters() instanceof GigaChatChatRequestParameters gigaChatParameters
                ? gigaChatParameters.getSessionId()
                : defaultChatRequestParameters.getSessionId();
        if (useV2) {
            doChatV2(chatRequest, handler, sessionId);
            return;
        }
        
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

    private void doChatV2(ChatRequest chatRequest, StreamingChatResponseHandler handler, String sessionId) {
        var responseMetadataBuilder = ChatResponseMetadata.builder();
        var text = new StringBuffer();
        var toolExecutionRequests = new ArrayList<ToolExecutionRequest>();

        try {
            CompletionRequestV2 requestV2 = toRequestV2(chatRequest);
            requestV2 = requestV2.toBuilder().stream(true).build();

            asyncClient.completionsV2Stream(requestV2, sessionId,
                    new CompletionV2StreamHandler() {
                        @Override
                        public void onMessageDelta(CompletionMessageDeltaEventV2 event) {
                            if (event.messages() != null) {
                                event.messages().forEach(message -> {
                                    if (message.content() != null) {
                                        message.content().forEach(part -> {
                                            if (part.text() != null) {
                                                text.append(part.text());
                                                handler.onPartialResponse(part.text());
                                            }
                                            if (part.functionCall() != null) {
                                                var functionCall = part.functionCall();
                                                String arguments;
                                                try {
                                                    arguments = functionCall.arguments() != null
                                                            ? chat.giga.util.JsonUtils.objectMapper()
                                                            .writeValueAsString(functionCall.arguments())
                                                            : "{}";
                                                } catch (Exception e) {
                                                    arguments = functionCall.arguments() != null
                                                            ? functionCall.arguments().toString() : "{}";
                                                }
                                                var toolRequest = ToolExecutionRequest.builder()
                                                        .name(functionCall.name())
                                                        .arguments(arguments)
                                                        .build();
                                                toolExecutionRequests.add(toolRequest);
                                            }
                                        });
                                    }
                                });
                            }
                        }

                        @Override
                        public void onMessageDone(CompletionMessageDoneEventV2 event) {
                            if (event.model() != null) {
                                responseMetadataBuilder.modelName(event.model());
                            }
                            if (event.finishReason() != null) {
                                responseMetadataBuilder.finishReason(finishReasonFromV2(event.finishReason()));
                            }
                            if (event.usage() != null) {
                                responseMetadataBuilder.tokenUsage(toTokenUsageV2(event.usage()));
                            }
                        }

                        @Override
                        public void onToolInProgress(CompletionToolLifecycleEventV2 event) {
                            // Handle tool lifecycle events if needed
                        }

                        @Override
                        public void onToolCompleted(CompletionToolLifecycleEventV2 event) {
                            // Handle tool lifecycle events if needed
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
                                            "No text or toolExecutionRequests found in the v2 response");
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
    public GigaChatChatRequestParameters defaultRequestParameters() {
        return defaultChatRequestParameters;
    }

}
