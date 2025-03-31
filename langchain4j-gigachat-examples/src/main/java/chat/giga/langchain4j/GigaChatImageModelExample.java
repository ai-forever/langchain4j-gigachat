package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;

import java.net.http.HttpClient;

public class GigaChatImageModelExample {

    public static void main(String args[]) {
        try {

            GigaChatImageModel model = GigaChatImageModel.builder()
                    .modelName(ModelName.GIGA_CHAT_PRO)
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
                    .apiUrl("host")
                    .build();

            System.out.println(model.generate("Нарисуй розового кота"));

        } catch (HttpClientException ex) {
            System.out.println(ex.statusCode() + ex.bodyAsString());
            ex.printStackTrace();
        }
    }
}
