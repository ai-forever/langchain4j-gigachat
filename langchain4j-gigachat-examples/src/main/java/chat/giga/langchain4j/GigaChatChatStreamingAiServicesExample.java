package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;

import java.util.concurrent.TimeUnit;

public class GigaChatChatStreamingAiServicesExample {

    public static void main(String[] args) {
        try {
            GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey("testkey")
                                    .build())
                            .build())
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .responseFormat(JsonSchema.builder()
                                    .rootElement(new JsonStringSchema())
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
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

        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
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
