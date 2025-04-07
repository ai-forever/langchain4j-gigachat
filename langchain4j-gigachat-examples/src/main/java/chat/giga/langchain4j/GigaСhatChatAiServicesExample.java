package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
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
                                    .clientId("testClientId")
                                    .clientSecret("testClientSecret")
                                    .build())
                            .build())
                    .defaultChatRequestParameters((DefaultChatRequestParameters) DefaultChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .responseFormat(JsonSchema.builder().rootElement(new JsonStringSchema()).build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("testApiUrl")
                    .build();
            var calculator = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .tools(new Calculator())
                    .build();

            double number = 16;
            String result = calculator.chat("Квадратный корень из " + number);
            System.out.println(result);
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
