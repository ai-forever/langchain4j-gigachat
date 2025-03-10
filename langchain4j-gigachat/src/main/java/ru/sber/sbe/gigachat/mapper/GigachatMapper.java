package ru.sber.sbe.gigachat.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.output.FinishReason;
import dev.langchain4j.model.output.TokenUsage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import ru.sber.sbe.gigachat.internal.api.model.completions.*;
import ru.sber.sbe.gigachat.tool.GigaChatToolSpecification;
import ru.sber.sbe.gigachat.tool.ToolExample;
import ru.sber.sbe.gigachat.usage.GigaUsage;

import java.util.*;
import java.util.stream.Collectors;

import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.model.output.FinishReason.LENGTH;
import static dev.langchain4j.model.output.FinishReason.STOP;
import static java.util.Objects.nonNull;

/**
 * @author protas-nv 17.09.2024
 */
@Slf4j
public class GigachatMapper {

    static ObjectMapper om = new ObjectMapper();

    public static List<ChatFunctionsInner> toGigachatFunctions(List<ToolSpecification> toolSpecifications) {
        if (toolSpecifications == null) {
            return null;
        }
        return toolSpecifications.stream().map(GigachatMapper::toGigachatFunction).collect(Collectors.toList());
    }

    private static ChatFunctionsInner toGigachatFunction(ToolSpecification toolSpecification) {
        var params = toolSpecification.parameters();
        return ChatFunctionsInner.builder()
                                 .name(toolSpecification.name())
                                 .description(toolSpecification.description())
                                 .parameters(GigachatToolSchema.builder()
                                                               .properties(params != null ? params.properties() : Collections.emptyMap())
                                                               .required(params != null ? params.required() : Collections.emptyList())
                                                               .build())
//                .returnParameters() // TODO(nvprotas): 20.09.2024 Расширить ToolSpecification?
//                .fewShotExamples() // TODO(nvprotas): 20.09.2024 Расширить ToolSpecification?
                                 .build();
    }

    public static List<ChatFunctionsInner> _toGigachatFunctions(List<GigaChatToolSpecification> toolSpecifications) {
        if (toolSpecifications == null) {
            return null;
        }
        return toolSpecifications.stream().map(GigachatMapper::toGigachatFunction).collect(Collectors.toList());
    }

    private static ChatFunctionsInner toGigachatFunction(GigaChatToolSpecification toolSpecification) {
        var returns = toolSpecification.returnParameters();
        var params = toolSpecification.parameters();
        return ChatFunctionsInner.builder()
                                 .name(toolSpecification.name())
                                 .description(toolSpecification.description())
                                 .parameters(GigachatToolSchema.builder()
                                                               .properties(params != null ? params.properties() : Collections.emptyMap())
                                                               .required(params != null ? params.required() : Collections.emptyList())
                                                               .build())
                                 .returnParameters(returns)
                                 .fewShotExamples(_toFewShotExamples(toolSpecification.usageExamples()))
                                 .build();
    }

    private static List<ChatFunctionsInnerFewShotExamplesInner> _toFewShotExamples(List<ToolExample> usageExamples) {
        if (usageExamples == null || usageExamples.isEmpty()) {
            return null;
        }
        return usageExamples.stream().map(GigachatMapper::_toFewShotExample).collect(Collectors.toList());
    }

    private static ChatFunctionsInnerFewShotExamplesInner _toFewShotExample(ToolExample toolExample) {
        return ChatFunctionsInnerFewShotExamplesInner.builder()
                                                     .request(toolExample.getRequestExample())
                                                     .params(toolExample.getParams())
                                                     .build();
    }

    public static List<Message> toGigachatMessages(List<ChatMessage> messages) {
        List<Message> gigachatMessages = new ArrayList<>();
        for (ChatMessage message : messages) {
            if (message instanceof SystemMessage) {
                String contents = toGigachatMessageContents((SystemMessage) message);
                final Message build = Message.builder().content(contents).role(RoleEnum.SYSTEM).build();
                gigachatMessages.add(build);
            } else if (message instanceof UserMessage) {
                String contents = toGigachatMessageContents((UserMessage) message);
                final Message build = Message.builder().content(contents).role(RoleEnum.USER).build();
                gigachatMessages.add(build);
            } else if (message instanceof AiMessage) {
                final AiMessage aiMessage = (AiMessage) message;
                String text = toGigachatMessageContents(aiMessage);
                FunctionCallRequest fcr = toFunctionCallRequest(aiMessage.toolExecutionRequests());
                final Message build = Message.builder().content(text).role(RoleEnum.ASSISTANT).functionCall(fcr).build();
                gigachatMessages.add(build);
            } else if (message instanceof ToolExecutionResultMessage) {
                final ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) message;
                String contents = toGigachatMessageContents(toolMsg);
                final Message build = Message.builder().content(contents).role(RoleEnum.FUNCTION).name((toolMsg).toolName()).build();
                gigachatMessages.add(build);
            } else {
                log.warn("Message with type {} are not supported.", message.type()); // TODO(nvprotas): 19.09.2024
            }
        }
        return gigachatMessages;
    }

    private static String toGigachatMessageContents(SystemMessage message) {
        return message.text();
    }

    private static String toGigachatMessageContents(UserMessage message) {
        return message.text();
    }

    private static String toGigachatMessageContents(AiMessage message) {
        return message.text();
    }

    @SneakyThrows
    private static FunctionCallRequest toFunctionCallRequest(List<ToolExecutionRequest> toolExecutionRequests) {
        if (toolExecutionRequests == null || toolExecutionRequests.isEmpty()) {
            return null;
        }
        ToolExecutionRequest toolExecutionRequest = toolExecutionRequests.get(0);
        final Map<String, Object> arguments = om.readValue(toolExecutionRequest.arguments(), Map.class);
        return FunctionCallRequest.builder().name(toolExecutionRequest.name()).arguments(arguments).build();
    }

    private static String toGigachatMessageContents(ToolExecutionResultMessage message) {
        return message.text();
    }

    public static AiMessage toAiMessage(GigaChatResponse gigaChatResponse) {
        final String text = gigaChatResponse.getChoices().get(0).getMessage().getContent();
        ToolExecutionRequest toolExecutionRequest =
                toToolExecutionRequest(gigaChatResponse.getChoices().get(0).getMessage().getFunctionCall());

        if (isNotNullOrBlank(text) && nonNull(toolExecutionRequest)) {
            return new AiMessage(text, List.of(toolExecutionRequest));
        } else if (nonNull(toolExecutionRequest)) {
            return AiMessage.from(List.of(toolExecutionRequest));
        } else {
            return AiMessage.from(text);
        }
    }

    @SneakyThrows
    public static ToolExecutionRequest toToolExecutionRequest(FunctionCallRequest fcr) {
        if (fcr == null) {
            return null;
        }
        return ToolExecutionRequest.builder().name(fcr.getName()).arguments(om.writeValueAsString(fcr.getArguments())).build();
    }

    public static TokenUsage toTokenUsage(GigaChatResponse gigaChatResponse) {
        return new GigaUsage(gigaChatResponse.getUsage().getPromptTokens(),
                gigaChatResponse.getUsage().getCompletionTokens(),
                gigaChatResponse.getUsage().getPrecachedPromptTokens());
    }

    public static FinishReason toFinishReason(GigaChatResponse gigaChatResponse) {
        FinishReasonEnum stopReason = gigaChatResponse.getChoices().get(0).getFinishReason();
        if (stopReason == null) {
            return null;
        }
        switch (stopReason) {
            case STOP:
                return STOP;
            case LENGTH:
                return LENGTH;
            case FUNCTION_CALL:
                return FinishReason.TOOL_EXECUTION;
            default:
                return null; // TODO
        }
    }
}