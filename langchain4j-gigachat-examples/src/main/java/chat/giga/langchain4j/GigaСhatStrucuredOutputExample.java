package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ResponseFormat;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchema;

import static dev.langchain4j.model.chat.request.ResponseFormatType.JSON;


public class GigaСhatStrucuredOutputExample {

    public static void main(String[] args) {
        try {

            String raw_schema = """
                    {"type":"object","properties":{"name":{"type":"string"},"age":{"type":"integer"},"height":{"type":"number"},"married":{"type":"boolean"}},"required":["name","age","height","married"],"additionalProperties":false},"strict":true}
                    """;
            ResponseFormat responseFormatRaw = ResponseFormat.builder()
                    .type(JSON)
                    .jsonSchema(JsonSchema.builder()
                            .name("Person")
                            .rootElement(JsonRawSchema.builder().schema(raw_schema).build())
                            .build())
                    .build();

            ResponseFormat responseFormatObject = ResponseFormat.builder()
                    .type(JSON)
                    .jsonSchema(JsonSchema.builder()
                            .name("Person")
                            .rootElement(
                                    JsonObjectSchema.builder()
                                            .addStringProperty("name")
                                            .addIntegerProperty("age")
                                            .addNumberProperty("height")
                                            .addBooleanProperty("married")
                                            .required("name", "age", "height", "married")
                                            .build())
                            .build())
                    .build();

            GigaChatChatModel model = GigaChatChatModel.builder()
                    .strictJsonSchema(true)
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_MAX_2 + "-preview")
                            .build())
                    .apiUrl("https://gigachat-ift.sberdevices.delta.sbrf.ru/v1")
                    .authClient(AuthClient.builder()
                            .withCertificatesAuth(new JdkHttpClientBuilder()
                                    .ssl(SSL.builder()
                                            .truststorePassword(System.getenv("TRUST_PASSWORD"))
                                            .trustStoreType("PKCS12")
                                            .truststorePath(System.getenv("TRUST_PATH"))
                                            .keystorePassword(System.getenv("KEY_PASSWORD"))
                                            .keystoreType("PKCS12")
                                            .keystorePath(System.getenv("KEY_PATH"))
                                            .build())
                                    .build())
                            .build())
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            UserMessage userMessage = UserMessage.from("""
                    John is 42 years old and lives an independent life.
                    He stands 1.75 meters tall and carries himself with confidence.
                    Currently unmarried, he enjoys the freedom to focus on his personal goals and interests.
                    """);

            model.chat(ChatRequest.builder()
                    .responseFormat(responseFormatRaw)
                    .messages(userMessage)
                    .build());

            model.chat(ChatRequest.builder()
                    .responseFormat(responseFormatObject)
                    .messages(userMessage)
                    .build());
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
