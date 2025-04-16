package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;

public class GigaСhatChatModelExample {

    public static void main(String[] args) {
        try {
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO)
                            .build())
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey("testkey")
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            model.chat("как дела");
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
