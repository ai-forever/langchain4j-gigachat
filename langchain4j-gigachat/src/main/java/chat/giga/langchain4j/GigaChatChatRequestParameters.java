package chat.giga.langchain4j;

import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import lombok.Getter;

import java.util.List;

import static dev.langchain4j.internal.Utils.getOrDefault;

@Getter
public class GigaChatChatRequestParameters extends DefaultChatRequestParameters {

    private final Integer updateInterval;
    private final Boolean stream;
    private final Boolean profanityCheck;
    private final Object functionCall;
    private final List<String> attachments;
    private final Float repetitionPenalty;
    private final String sessionId;
    private final Boolean strictJsonSchema;
    private final Boolean useV2Completions;
    private final Boolean disableFilter;
    private final String assistantId;
    private final String memoryId;
    private final List<String> flags;
    private final String reasoningEffort;

    private GigaChatChatRequestParameters(GigaChatBuilder builder) {
        super(builder);
        this.updateInterval = builder.updateInterval;
        this.stream = builder.stream;
        this.profanityCheck = builder.profanityCheck;
        this.functionCall = builder.functionCall;
        this.attachments = builder.attachments;
        this.repetitionPenalty = builder.repetitionPenalty;
        this.sessionId = builder.sessionId;
        this.strictJsonSchema = builder.strictJsonSchema;
        this.useV2Completions = builder.useV2Completions;
        this.disableFilter = builder.disableFilter;
        this.assistantId = builder.assistantId;
        this.memoryId = builder.memoryId;
        this.flags = builder.flags;
        this.reasoningEffort = builder.reasoningEffort;
    }

    public static class GigaChatBuilder extends Builder<GigaChatBuilder> {

        private Integer updateInterval;
        private Boolean stream = false;
        private Boolean profanityCheck = false;
        private Object functionCall;
        private List<String> attachments;
        private Float repetitionPenalty;
        private String sessionId;
        private Boolean strictJsonSchema = false;
        private Boolean useV2Completions = false;
        private Boolean disableFilter;
        private String assistantId;
        private String memoryId;
        private List<String> flags;
        private String reasoningEffort;

        public GigaChatBuilder updateInterval(Integer updateInterval) {
            this.updateInterval = updateInterval;
            return this;
        }
        public GigaChatBuilder profanityCheck(Boolean profanityCheck) {
            this.profanityCheck = profanityCheck;
            return this;
        }
        public GigaChatBuilder functionCall(Object functionCall) {
            this.functionCall = functionCall;
            return this;
        }
        public GigaChatBuilder attachments(List<String> attachments) {
            this.attachments = attachments;
            return this;
        }

        public GigaChatBuilder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public GigaChatBuilder repetitionPenalty(Float repetitionPenalty) {
            this.repetitionPenalty = repetitionPenalty;
            return this;
        }

        public GigaChatBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public GigaChatBuilder strictJsonSchema(Boolean strictJsonSchema) {
            this.strictJsonSchema = strictJsonSchema;
            return this;
        }

        public GigaChatBuilder useV2Completions(Boolean useV2Completions) {
            this.useV2Completions = useV2Completions;
            return this;
        }

        public GigaChatBuilder disableFilter(Boolean disableFilter) {
            this.disableFilter = disableFilter;
            return this;
        }

        public GigaChatBuilder assistantId(String assistantId) {
            this.assistantId = assistantId;
            return this;
        }

        public GigaChatBuilder memoryId(String memoryId) {
            this.memoryId = memoryId;
            return this;
        }

        public GigaChatBuilder flags(List<String> flags) {
            this.flags = flags;
            return this;
        }

        public GigaChatBuilder reasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
            return this;
        }

        @Override
        public GigaChatChatRequestParameters build() {
            return new GigaChatChatRequestParameters(this);
        }

        @Override
        public GigaChatBuilder overrideWith(ChatRequestParameters parameters) {
            super.overrideWith(parameters);
            if (parameters instanceof GigaChatChatRequestParameters chatChatRequestParameters) {
                updateInterval(getOrDefault(chatChatRequestParameters.getUpdateInterval(), updateInterval));
                profanityCheck(getOrDefault(chatChatRequestParameters.getProfanityCheck(), profanityCheck));
                functionCall(getOrDefault(chatChatRequestParameters.getFunctionCall(), functionCall));
                attachments(getOrDefault(chatChatRequestParameters.getAttachments(), attachments));
                stream(getOrDefault(chatChatRequestParameters.getStream(), stream));
                repetitionPenalty(getOrDefault(chatChatRequestParameters.getRepetitionPenalty(), repetitionPenalty));
                sessionId(getOrDefault(chatChatRequestParameters.getSessionId(), sessionId));
                strictJsonSchema(getOrDefault(chatChatRequestParameters.getStrictJsonSchema(), strictJsonSchema));
                useV2Completions(getOrDefault(chatChatRequestParameters.getUseV2Completions(), useV2Completions));
                disableFilter(getOrDefault(chatChatRequestParameters.getDisableFilter(), disableFilter));
                assistantId(getOrDefault(chatChatRequestParameters.getAssistantId(), assistantId));
                memoryId(getOrDefault(chatChatRequestParameters.getMemoryId(), memoryId));
                flags(getOrDefault(chatChatRequestParameters.getFlags(), flags));
                reasoningEffort(getOrDefault(chatChatRequestParameters.getReasoningEffort(), reasoningEffort));
            }
            return this;
        }
    }

    @Override
    public GigaChatChatRequestParameters overrideWith(ChatRequestParameters that) {
        return GigaChatChatRequestParameters.builder()
                .overrideWith(this)
                .overrideWith(that)
                .build();
    }

    public static GigaChatBuilder builder() {
        return new GigaChatBuilder();
    }
}
