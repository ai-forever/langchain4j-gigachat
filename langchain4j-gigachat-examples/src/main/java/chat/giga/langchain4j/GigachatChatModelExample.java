package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;

import java.net.http.HttpClient;

public class GigachatChatModelExample {

    public static void main(String[] args) {
        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withCertificatesAuth(new JdkHttpClientBuilder()
                                    .httpClientBuilder(HttpClient.newBuilder())
                                    .ssl(SSL.builder()
                                            .truststorePassword("pass")
                                            .trustStoreType("PKCS12")
                                            .truststorePath("/Users/user/ssl/client_truststore.p12")
                                            .keystorePassword("pass")
                                            .keystoreType("PKCS12")
                                            .keystorePath("/Users/user/ssl/client_keystore.p12")
                                            .build())
                                    .build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("host1")
                    .build();
            model.chat(ChatRequest.builder().messages(new UserMessage("как дела"))
                    .parameters(DefaultChatRequestParameters.builder().modelName(ModelName.GIGA_CHAT_PRO).build()).build());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
