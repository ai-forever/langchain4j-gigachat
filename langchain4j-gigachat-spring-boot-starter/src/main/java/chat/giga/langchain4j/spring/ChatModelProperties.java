package chat.giga.langchain4j.spring;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.util.List;

@Data
public class ChatModelProperties {

    @NestedConfigurationProperty
    private AuthProperties auth;
    private String apiUrl;
    private String modelName;
    private boolean profanityCheck;
    private boolean verifySslCerts;
    private boolean logRequests;
    private boolean logResponses;
    private Double temperature;
    private Double topP;
    private List<String> stop;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Double presencePenalty;
    private Double frequencyPenalty;
    private Integer timeout;
    private Integer maxRetries;
}
