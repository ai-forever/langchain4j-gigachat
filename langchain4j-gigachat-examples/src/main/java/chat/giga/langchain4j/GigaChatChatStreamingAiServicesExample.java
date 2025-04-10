package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.net.http.HttpClient;
import java.util.concurrent.TimeUnit;

public class GigaChatChatStreamingAiServicesExample {

    public static void main(String[] args) {
        try {
            GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
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
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .responseFormat(JsonSchema.builder().rootElement(new JsonStringSchema()).build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("test.ru/v1")
                    .build();
            var calculator = AiServices.builder(Assistant.class)
                    .streamingChatLanguageModel(model)
                    .tools(new GigaChatChatStreamingAiServicesExample.Calculator())
                    .build();

            double number = 16;
            TokenStream result = calculator.chat("Квадратный корень из " + number);

            result.onPartialResponse(System.out::println)
                    .onCompleteResponse(System.out::println)
                    .onError(Throwable::printStackTrace)
                    .start();

            TimeUnit.MINUTES.sleep(1);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    interface Assistant {

        TokenStream chat(String message);
    }

    static class Calculator {

        @Tool("Возвращает квадратный корень из числа")
        double squareRoot(double x) {
            return Math.sqrt(x);
        }
    }
}
