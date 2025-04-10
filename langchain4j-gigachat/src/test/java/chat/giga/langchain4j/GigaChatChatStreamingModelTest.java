package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.sse.SseListener;
import chat.giga.model.ModelName;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.DefaultChatRequestParameters;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GigaChatChatStreamingModelTest {

    @Mock
    chat.giga.http.client.HttpClient httpClient;
    @Mock
    AuthClient authClient;

    @Mock
    StreamingChatResponseHandler completionChunkResponseHandler;

    GigaChatStreamingChatModel model;

    ObjectMapper objectMapper = JsonUtils.objectMapper();

    @BeforeEach
    void setUp() {
        model = GigaChatStreamingChatModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("hostTest")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .temperature(1.0)
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .responseFormat(JsonSchema.builder().build())
                        .build())
                .build();
    }

    @Test
    void chat() {

        var body = TestData.completionChunkResponse();
        doAnswer(i -> {
            var listener = i.getArgument(1, SseListener.class);
            listener.onError(new HttpClientException(401, null));

            return null;
        }).doAnswer(i -> {
            var listener = i.getArgument(1, SseListener.class);
            listener.onData(objectMapper.writeValueAsString(body));
            listener.onComplete();

            return null;
        }).when(httpClient).execute(any(), any());

        model.chat(
                ChatRequest.builder()
                        .messages(new UserMessage("Получить положительное значение квадратного корня из числа 25"))
                        .parameters(DefaultChatRequestParameters.builder().modelName(ModelName.GIGA_CHAT_PRO)
                                .build())
                        .build(), completionChunkResponseHandler);

        var responseCaptor = ArgumentCaptor.forClass(String.class);
        var responseCaptorFinal = ArgumentCaptor.forClass(ChatResponse.class);

        verify(completionChunkResponseHandler, timeout(300)).onPartialResponse(responseCaptor.capture());
        verify(completionChunkResponseHandler, timeout(100)).onCompleteResponse(responseCaptorFinal.capture());
        verify(completionChunkResponseHandler, after(100).never()).onError(any());

        assertThat(responseCaptor.getValue()).isEqualTo(body.choices().get(0).delta().content());
        assertThat(responseCaptorFinal.getValue().aiMessage().text()).isEqualTo(
                body.choices().get(0).delta().content());
        assertThat(responseCaptorFinal.getValue().finishReason().name().toLowerCase()).isEqualTo(
                body.choices().get(0).finishReason().value());

    }
}
