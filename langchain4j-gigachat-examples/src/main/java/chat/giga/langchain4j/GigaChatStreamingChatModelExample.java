package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.TimeUnit;

public class GigaChatStreamingChatModelExample {

    public static void main(String[] args) throws InterruptedException {
        try {
            GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .clientId("clientId")
                                    .clientSecret("clientSecret")
                                    .build())
                            .build())
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .temperature(1.0)
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .responseFormat(JsonSchema.builder().build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            model.chat("Напиши сочинение про науку и жизнь 5000 символов", new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String s) {
                    System.out.println(s);
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    System.out.println(chatResponse);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println(throwable.getMessage());
                    throwable.printStackTrace();
                }
            });
        } catch (HttpClientException ex) {
            System.out.println(ex.statusCode() + ex.bodyAsString());
            ex.printStackTrace();
        }

        TimeUnit.MINUTES.sleep(1);
    }
}
