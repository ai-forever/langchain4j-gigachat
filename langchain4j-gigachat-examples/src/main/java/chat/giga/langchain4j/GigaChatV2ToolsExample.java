package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;

import java.util.List;

/**
 * Продвинутый пример использования GigaChat API v2 с поддержкой инструментов (tools).
 * <p>
 * В этом примере демонстрируются возможности API v2:
 * <ul>
 *   <li>Использование инструментов (tools) в API v2</li>
 *   <li>JSON Schema для структурированных ответов</li>
 *   <li>Строгая валидация схемы</li>
 *   <li>Работа с расширенными параметрами модели</li>
 * </ul>
 *
 * <p><strong>Особенности работы с инструментами в API v2:</strong>
 * <ul>
 *   <li>Улучшенная поддержка сложных схем инструментов</li>
 *   <li>Поддержка AnyOf, AllOf, OneOf схем</li>
 *   <li>Расширенные возможности валидации</li>
 * </ul>
 */
public class GigaChatV2ToolsExample {

    public static void main(String[] args) {
        try {
            // Создаем простую спецификацию инструмента
            // В API v2 используется JsonObjectSchema для параметров инструментов
            ToolSpecification weatherTool = ToolSpecification.builder()
                    .name("get_weather")
                    .description("Получить текущую погоду в указанном городе")
                    .parameters(JsonObjectSchema.builder()
                            .addStringProperty("city")
                            .addStringProperty("unit")
                            .build())
                    .build();

            // Создаем модель с использованием API v2 и инструментов
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey(System.getenv("AUTH_KEY"))
                                    .build())
                            .build())
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO_2)
                            // Включаем использование API v2
                            .useV2Completions(true)
                            // Настраиваем инструменты
                            .toolSpecifications(List.of(weatherTool))
                            // Reasoning режим для сложных задач
                            .reasoningEffort("medium")
                            // Температура для баланса между креативностью и точностью
                            .temperature(0.7)
                            .build())
                    // URL для API v2
                    .apiUrl(System.getenv("API_URL"))
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            System.out.println("=== ПРИМЕР: Запрос с инструментом погоды ===");
            System.out.println("Запрос: 'Какая погода сегодня в Москве?'");

            var response = model.chat("Какая погода сегодня в Москве?");

            System.out.println("Ответ модели:");
            System.out.println(response);

            System.out.println("\n=== ПРИМЕР: Сложный запрос ===");
            System.out.println("Запрос: 'Если в Москве +20°C, а в Нью-Йорке 68°F, где теплее?'");

            response = model.chat("Если в Москве +20°C, а в Нью-Йорке 68°F, где теплее?");

            System.out.println("Ответ модели:");
            System.out.println(response);

        } catch (HttpClientException ex) {
            System.out.println("Ошибка HTTP: код " + ex.statusCode() + ", ответ: " + ex.bodyAsString());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Общая ошибка:");
            ex.printStackTrace();
        }
    }
}
