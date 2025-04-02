package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.service.AiServices;

public class GigachatChatAiServicesExample {

    public static void main(String[] args) {
        try {
            GigachatChatModel model = GigachatChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .clientId("testClientId")
                                    .clientSecret("testClientSecret")
                                    .build())
                            .build())
                    .defaultChatRequestParameters((DefaultChatRequestParameters) DefaultChatRequestParameters.builder()
                            .temperature(1.0)
                            .responseFormat(JsonSchema.builder().build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("testApiUrl")
                    .build();
            var s = AiServices.builder(Assistant.class)
                    .chatLanguageModel(model)
                    .tools(new Calculator())
                    .build();

            var answer = s.chat("What is the square root of 475695037565?");

            System.out.println(answer);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    interface Assistant {

        String chat(String message);
    }

    static class Calculator {

        @Tool
        double add(int a, int b) {
            return a + b;
        }

        @Tool
        double squareRoot(double x) {
            return Math.sqrt(x);
        }
    }
}
