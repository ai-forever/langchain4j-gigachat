package chat.giga.langchain4j.spring;

import chat.giga.model.Scope;
import lombok.Data;

@Data
public class AuthProperties {

    private AuthType type;
    private String authKey;
    private String clientId;
    private String clientSecret;
    private Scope scope;
    private String authApiUrl;
    private boolean verifySslCerts;
    private String user;
    private String password;
    private String accessToken;
    private String trustStorePassword;
    private String trustStoreType;
    private String keyStoreType;
    private String trustStorePath;
    private String keyStorePath;
    private String keyStorePassword;
    private Integer timeout;
}
