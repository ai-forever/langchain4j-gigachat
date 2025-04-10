package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpResponse;
import chat.giga.model.ModelName;
import chat.giga.model.completion.Choice;
import chat.giga.model.completion.ChoiceMessage;
import chat.giga.model.completion.CompletionResponse;
import chat.giga.model.completion.Usage;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.chat.request.json.JsonSchema;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GigaChatImageModelTest {

    @Mock
    chat.giga.http.client.HttpClient httpClient;
    @Mock
    AuthClient authClient;

    GigaChatImageModel model;

    ObjectMapper objectMapper = JsonUtils.objectMapper();

    @BeforeEach
    void setUp() {
        model = GigaChatImageModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("host")
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .temperature(1.0)
                        .modelName(ModelName.GIGA_CHAT_PRO)
                        .responseFormat(JsonSchema.builder().build())
                        .build())
                .build();
    }

    @Test
    void generate() throws JsonProcessingException {
        var body = TestData.completionResponse();

        when(httpClient.execute(any()))
                .thenReturn(HttpResponse.builder()
                        .body(objectMapper.writeValueAsBytes(body))
                        .build());

        Response<Image> response = model.generate("нарисуй кота");
        System.out.println(response);
        assertNotNull(response.content().base64Data());
    }

    @Test
    void generateException() throws JsonProcessingException {

        when(httpClient.execute(any()))
                .thenReturn(HttpResponse.builder()
                        .body(objectMapper.writeValueAsBytes(
                                CompletionResponse.builder().usage(Usage.builder().build()).choices(List.of(
                                        Choice.builder().message(ChoiceMessage.builder().build()).build())).build()))
                        .build());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> model.generate("нарисуй кота"));

        assertEquals("No image was generated response is null", exception.getMessage());
    }
}
