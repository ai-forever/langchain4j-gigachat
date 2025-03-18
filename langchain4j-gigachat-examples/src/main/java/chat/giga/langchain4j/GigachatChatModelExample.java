package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.Scope;
import dev.langchain4j.data.message.UserMessage;

import java.util.Collections;

public class GigachatChatModelExample {

    public static void main(String args[]) {
        GigaChatClient client = GigaChatClient.builder()
                .verifySslCerts(false)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey("key")
                                .build())
                        .build())
                .build();
        try {
            GigachatChatModel model = GigachatChatModel.builder().client(client).build();
            model.chat(Collections.singletonList(new UserMessage("Как дела?")));

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
