package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpResponse;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
