package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import chat.giga.model.completion.ChatFunctionCallEnum;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.AiServices;

public class GigaСhatChatAiServicesExample {

    public static void main(String[] args) {
        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey("key")
                                    .build())
                            .build())
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .responseFormat(JsonSchema.builder()
                                    .rootElement(new JsonStringSchema())
                                    .build())
                            .functionCall(ChatFunctionCallEnum.AUTO)
                            .updateInterval(2)
                            .profanityCheck(false)
                            .repetitionPenalty(2.0f)
                            .stream(false)
                            .temperature(1.0)
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            var calculator = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .tools(new Calculator())
                    .build();

            double number = 16;
            String result = calculator.chat("Квадратный корень из " + number);
            System.out.println(result);
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    interface Assistant {
        String chat(String message);
    }

    static class Calculator {

        @Tool("Возвращает квадратный корень из числа")
        double squareRoot(double x) {
            return Math.sqrt(x);
        }
    }
}
