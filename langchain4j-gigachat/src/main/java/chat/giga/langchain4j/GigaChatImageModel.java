package chat.giga.langchain4j;

import chat.giga.client.GigaChatClient;
import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.output.Response;
import lombok.Builder;

import java.util.Base64;
import java.util.List;

import static dev.langchain4j.internal.RetryUtils.withRetry;
import static dev.langchain4j.internal.Utils.getOrDefault;

public class GigaChatImageModel implements ImageModel {

    private final String modelName;
    private final GigaChatClient client;
    private final Integer maxRetries;
    private GigachatChatModel chatModel;

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
                              String modelName) {
        chatModel = GigachatChatModel.builder()
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

        this.modelName = modelName;
        this.maxRetries = getOrDefault(maxRetries, 1);
    }


    @Override
    public Response<Image> generate(String userMessage) {
        ChatResponse response = chatModel.doChat(ChatRequest.builder()
                .parameters(ChatRequestParameters.builder().modelName(modelName).build())
                .messages(new ChatMessage[]{UserMessage.from(userMessage)}).build());

        var completionsResponse = response.aiMessage().text();
        if (completionsResponse != null) {
            if (completionsResponse != null && completionsResponse.contains("img src=")) {
                var fileId = completionsResponse.split("\"")[1];
                byte[] file = withRetry(() -> client.downloadFile(fileId, null), maxRetries);
                String data = new String(Base64.getEncoder().encode(file));
                return Response.from(Image.builder().base64Data(data).build(), response.tokenUsage());
            } else {
                return Response.from(Image.builder().build(), response.tokenUsage());
            }
        } else {
            throw new RuntimeException("No image was generated response is null");
        }
    }
}
