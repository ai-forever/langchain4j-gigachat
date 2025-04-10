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

    private GigaChatChatRequestParameters(GigaChatBuilder builder) {
        super(builder);
        this.updateInterval = builder.updateInterval;
        this.stream = builder.stream;
        this.profanityCheck = builder.profanityCheck;
        this.functionCall = builder.functionCall;
        this.attachments = builder.attachments;
        this.repetitionPenalty = builder.repetitionPenalty;
    }

    public static class GigaChatBuilder extends Builder<GigaChatBuilder> {

        private Integer updateInterval;
        private Boolean stream;
        private Boolean profanityCheck;
        private Object functionCall;
        private List<String> attachments;
        private Float repetitionPenalty;

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
