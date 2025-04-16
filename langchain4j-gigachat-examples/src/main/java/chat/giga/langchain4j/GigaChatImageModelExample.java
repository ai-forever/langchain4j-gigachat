package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;

public class GigaChatImageModelExample {

    public static void main(String[] args) {
        try {

            GigaChatImageModel model = GigaChatImageModel.builder()
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .build())
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey("testkey")
                                    .build())
                            .build())
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .apiUrl("test.ru")
                    .build();

            System.out.println(model.generate("Нарисуй розового кота"));

        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
