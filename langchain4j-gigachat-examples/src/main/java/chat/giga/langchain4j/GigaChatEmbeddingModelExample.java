package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;

import java.net.http.HttpClient;

public class GigaChatEmbeddingModelExample {
    public static void main(String args[]) {
        try {
            GigaChatEmbeddingModel model = GigaChatEmbeddingModel.builder()
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
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("https://host")
                    .build();
            System.out.println(model.embed("тест"));
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
