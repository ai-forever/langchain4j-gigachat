package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.HttpResponse;
import chat.giga.util.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GigaChatEmbeddingModelTest {

    @Mock
    chat.giga.http.client.HttpClient httpClient;
    @Mock
    AuthClient authClient;

    GigaChatEmbeddingModel model;

    ObjectMapper objectMapper = JsonUtils.objectMapper();

    @BeforeEach
    void setUp() {
        model = GigaChatEmbeddingModel.builder()
                .authClient(authClient)
                .apiHttpClient(httpClient)
                .apiUrl("host")
                .build();
    }

    @Test
    void embed() throws JsonProcessingException {
        var body = TestData.embeddingResponse();

        when(httpClient.execute(any()))
                .thenThrow(new HttpClientException(401, null))
                .thenReturn(HttpResponse.builder()
                        .body(objectMapper.writeValueAsBytes(body))
                        .build());

        Response<Embedding> response = model.embed("тест");
        assertThat(response.tokenUsage().inputTokenCount()).isEqualTo(body.data().getFirst().usage().promptTokens());
        assertThat(response.content().vectorAsList()).containsAll(body.data().getFirst().embedding());
    }
}
