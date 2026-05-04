package chat.giga.langchain4j.spring;

import chat.giga.model.v2.completion.FilterConfigV2;
import chat.giga.model.v2.completion.RankerOptionsV2;
import chat.giga.model.v2.completion.UserInfoV2;
import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

@Data
public class ChatModelProperties {

    @NestedConfigurationProperty
    private AuthProperties auth;
    private String apiUrl;
    private String apiV2Url;
    private String modelName;
    private boolean profanityCheck;
    private boolean verifySslCerts;
    private boolean logRequests;
    private boolean logResponses;
    private boolean useV2Completions;
    private Double temperature;
    private Double topP;
    private List<String> stop;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double presencePenalty;
    private Double frequencyPenalty;
    private Integer timeout;
    private Integer maxRetries;
    private String reasoningEffort;
    private Boolean disableFilter;
    private String assistantId;
    private String memoryId;
    private List<String> flags;
    private FilterConfigV2 filterConfig;
    private RankerOptionsV2 rankerOptions;
    private UserInfoV2 userInfo;
}
