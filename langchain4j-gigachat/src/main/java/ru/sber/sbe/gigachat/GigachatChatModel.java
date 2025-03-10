package ru.sber.sbe.gigachat;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.*;
import ru.sber.sbe.gigachat.internal.api.model.completions.*;
import ru.sber.sbe.gigachat.internal.client.GigachatClient;
import ru.sber.sbe.gigachat.tool.GigaChatToolSpecification;

import java.time.Duration;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;
import static ru.sber.sbe.gigachat.Utils.defaultIfNull;
import static ru.sber.sbe.gigachat.mapper.GigachatMapper.*;
import static ru.sber.sbe.gigachat.sanitizer.MessageSanitizer.sanitizeMessages;

/**
 * @author protas-nv 16.09.2024
 */
public class GigachatChatModel implements ChatLanguageModel {

    private final GigachatClient client;
    private final ModelEnum modelName;
    private final Double temperature;
    private final Double topP;
    private final int maxTokens;
    private final int maxRetries;
    private final Double repetitionPenalty;
    private final Boolean profanityCheck;

    @Builder
    private GigachatChatModel(String baseUrl,
                              String modelName,
                              Double temperature,
                              Double topP,
                              Double repetitionPenalty,
                              Integer maxTokens,
                              Duration timeout,
                              Integer maxRetries,
                              Boolean logRequests,
                              Boolean logResponses,
                              String caBundleFile,
                              String keyFilePath,
                              String certFilePath,
                              Boolean profanityCheck) {
        this.profanityCheck = profanityCheck;
        this.client = GigachatClient.builder()
                                    .baseUrl(ensureNotBlank(baseUrl, "baseUrl cannot be empty."))
                                    .timeout(getOrDefault(timeout, Duration.ofSeconds(60)))
                                    .logRequests(getOrDefault(logRequests, false))
                                    .logResponses(getOrDefault(logResponses, false))
                                    .caBundleFile(caBundleFile)
                                    .keyFilePath(keyFilePath)
                                    .certFilePath(certFilePath)
                                    .build();
        this.modelName = ModelEnum.fromValue(getOrDefault(modelName, "GigaChat"));
        this.temperature = temperature;
        this.topP = topP;
        this.repetitionPenalty = repetitionPenalty;
        this.maxTokens = getOrDefault(maxTokens, 1024);
        this.maxRetries = getOrDefault(maxRetries, 3);
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages) {
        List<ChatMessage> sanitizedMessages = sanitizeMessages(messages);

        GigaChatRequest request = GigaChatRequest.builder()
                                                 .model(modelName)
                                                 .messages(toGigachatMessages(sanitizedMessages))
                                                 .maxTokens(maxTokens)
                                                 .stream(false)
                                                 .temperature(temperature)
                                                 .topP(topP)
                                                 .repetitionPenalty(repetitionPenalty)
                                                 .profanityCheck(profanityCheck)
                                                 .build();

        GigaChatResponse response = withRetry(() -> client.createMessage(request), maxRetries);

        return Response.from(toAiMessage(response), toTokenUsage(response), toFinishReason(response));
    }

    @Override
    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        List<ChatMessage> sanitizedMessages = sanitizeMessages(messages);

        GigaChatRequest request = GigaChatRequest.builder()
                                                 .model(modelName)
                                                 .messages(toGigachatMessages(sanitizedMessages))
                                                 .maxTokens(maxTokens)
                                                 .stream(false)
                                                 .temperature(temperature)
                                                 .topP(topP)
                                                 .functionCall(FunctionCallNoneAuto.AUTO)
                                                 .functions(toGigachatFunctions(toolSpecifications))
                                                 .repetitionPenalty(repetitionPenalty)
                                                 .profanityCheck(profanityCheck)
                                                 .build();

        GigaChatResponse response = withRetry(() -> client.createMessage(request), maxRetries);

        return Response.from(toAiMessage(response), toTokenUsage(response), toFinishReason(response));
    }

    public Response<AiMessage> _generate(List<ChatMessage> messages, List<GigaChatToolSpecification> toolSpecifications) {
        return _generate(messages, toolSpecifications, new Overrides());
    }

    public Response<AiMessage> _generate(List<ChatMessage> messages, List<GigaChatToolSpecification> toolSpecifications, Overrides o) {
        List<ChatMessage> sanitizedMessages = sanitizeMessages(messages);

        GigaChatRequest request = GigaChatRequest.builder()
                                                 .model(defaultIfNull(o.modelName, modelName))
                                                 .messages(toGigachatMessages(sanitizedMessages))
                                                 .maxTokens(defaultIfNull(o.maxTokens, maxTokens))
                                                 .stream(false)
                                                 .temperature(defaultIfNull(o.temperature, temperature))
                                                 .topP(defaultIfNull(o.topP, topP))
                                                 .functionCall(FunctionCallNoneAuto.AUTO)
                                                 .functions(_toGigachatFunctions(toolSpecifications))
                                                 .repetitionPenalty(defaultIfNull(o.repetitionPenalty, repetitionPenalty))
                                                 .profanityCheck(defaultIfNull(o.profanityCheck, profanityCheck))
                                                 .build();

        GigaChatResponse response = withRetry(() -> client.createMessage(request), maxRetries);

        return Response.from(toAiMessage(response), toTokenUsage(response), toFinishReason(response));
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Setter
    public static class Overrides {
        ModelEnum modelName;
        Integer maxTokens;
        Double temperature;
        Double topP;
        Double repetitionPenalty;
        Boolean profanityCheck;
        Integer maxRetries;
    }
}