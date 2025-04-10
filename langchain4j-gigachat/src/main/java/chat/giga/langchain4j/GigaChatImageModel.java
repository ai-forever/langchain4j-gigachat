package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;

import java.util.Base64;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * Представляет собой реализацию GigaChat языковой модели интерфейса для генерации картинок
 * <p> Описание параметров можно найти  <a href="https://developers.sber.ru/docs/ru/gigachat/api/reference/rest/post-chat">тут</a>
 */
public class GigaChatImageModel implements ImageModel {

    private final GigaChatClient client;
    private final Integer maxRetries;
    private final GigaChatChatModel chatModel;

    @Builder
    public GigaChatImageModel(HttpClient apiHttpClient,
                              AuthClient authClient,
                              Integer readTimeout,
                              Integer connectTimeout,
                              String apiUrl,
                              boolean logRequests,
                              boolean logResponses,
                              boolean verifySslCerts,
                              Tokenizer tokenizer,
                              Integer maxRetries,
                              List<ChatModelListener> listeners,
            GigaChatChatRequestParameters defaultChatRequestParameters) {
        chatModel = GigaChatChatModel.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .listeners(listeners)
                .tokenizer(tokenizer)
                .defaultChatRequestParameters(defaultChatRequestParameters)
                .build();

        this.client = GigaChatClient.builder()
                .apiHttpClient(apiHttpClient)
                .apiUrl(apiUrl)
                .authClient(authClient)
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .verifySslCerts(verifySslCerts)
                .build();

        this.maxRetries = getOrDefault(maxRetries, 1);
    }


    @Override
    public Response<Image> generate(String userMessage) {
        var response = chatModel.chat(ChatRequest.builder()
                .messages(UserMessage.from(userMessage))
                .build());

        var completionsResponse = response.aiMessage().text();
        if (completionsResponse != null) {
            if (completionsResponse.contains("img src=")) {

                var fileId = completionsResponse.split("\"")[1];

                var file = withRetry(() -> client.downloadFile(fileId, null), maxRetries);

                var base64FileData = new String(Base64.getEncoder().encode(file));

                return Response.from(Image.builder().base64Data(base64FileData).build(), response.tokenUsage());
            } else {
                throw new RuntimeException("No image was generated response does not contain 'img src='");
            }
        } else {
            throw new RuntimeException("No image was generated response is null");
        }
    }
}
