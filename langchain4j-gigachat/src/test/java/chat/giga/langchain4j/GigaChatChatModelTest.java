package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClient;
import chat.giga.http.client.HttpResponse;
import chat.giga.model.ModelName;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GigaChatChatModelTest {

    @Mock
    HttpClient httpClient;
    @Mock
    AuthClient authClient;

    GigaChatChatModel model;

    ObjectMapper objectMapper = JsonUtils.objectMapper();

    @BeforeEach
    void setUp() {
        model = GigaChatChatModel.builder()
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
    void chat() throws JsonProcessingException {
        var body = TestData.completionResponse();

        when(httpClient.execute(any()))
                .thenReturn(HttpResponse.builder()
                        .body(objectMapper.writeValueAsBytes(body))
                        .build());

        var response = model.chat(
                ChatRequest.builder()
                        .messages(new UserMessage("Получить положительное значение квадратного корня из числа 25"))
                        .parameters(GigaChatChatRequestParameters.builder().modelName(ModelName.GIGA_CHAT_PRO)
                                .build())
                        .build());
        assertNotNull(response);
        assertThat(response.tokenUsage().inputTokenCount()).isEqualTo(body.usage().promptTokens());
        assertThat(response.aiMessage().text()).isEqualTo(body.choices().get(0).message().content());
        assertThat(response.metadata().modelName()).isEqualTo(body.model());
        assertThat(response.metadata().finishReason().name()).isEqualTo(body.choices().get(0).finishReason().name());
    }

    @Test
    void chatIfException() {
        when(httpClient.execute(any()))
                .thenThrow(new UncheckedIOException(new IOException("some error")));

        Exception exception = assertThrows(UncheckedIOException.class, () -> model.chat(
                ChatRequest.builder()
                        .messages(new UserMessage("Получить положительное значение квадратного корня из числа 25"))
                        .parameters(GigaChatChatRequestParameters.builder().modelName(ModelName.GIGA_CHAT_PRO)
                                .build())
                        .build()));

        assertThat(exception.getMessage()).isEqualTo("java.io.IOException: some error");
    }
}
