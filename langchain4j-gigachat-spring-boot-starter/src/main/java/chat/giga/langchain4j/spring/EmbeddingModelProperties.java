package chat.giga.langchain4j.spring;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
public class EmbeddingModelProperties {

    @NestedConfigurationProperty
    private AuthProperties auth;
    private String apiUrl;
    private String modelName;
    private boolean verifySslCerts;
    private boolean logRequests;
    private boolean logResponses;
    private Integer timeout;
    private Integer maxRetries;
    private Integer batchSize;

}
