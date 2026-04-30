package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;

public class GigaСhatChatModelExample {

    public static void main(String[] args) {
        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO_2)
                            .build())
                    .apiUrl(System.getenv("API_URL"))
                    .authClient(AuthClient.builder()
                            .withCertificatesAuth(new JdkHttpClientBuilder()
                                    .ssl(SSL.builder()
                                            .truststorePassword(System.getenv("TRUST_PASSWORD"))
                                            .trustStoreType("PKCS12")
                                            .truststorePath(System.getenv("TRUST_PATH"))
                                            .keystorePassword(System.getenv("KEY_PASSWORD"))
                                            .keystoreType("PKCS12")
                                            .keystorePath(System.getenv("KEY_PATH"))
                                            .build())
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            model.chat("как дела");
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
