package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;

/**
 * Пример использования GigaChat API v2 с GigaChatChatModel.
 * <p>
 * В этом примере демонстрируется:
 * <ul>
 *   <li>Использование API v2 через параметр {@code useV2Completions = true}</li>
 *   <li>Настройка специфичных параметров для v2 (assistantId, memoryId, reasoningEffort)</li>
 *   <li>Аутентификация с использованием сертификатов</li>
 * </ul>
 *
 * <p><strong>Важно:</strong> Для работы с API v2 требуется аккаунт с соответствующими правами доступа.
 *
 * @see GigaChatChatModel
 * @see GigaChatChatRequestParameters
 */
public class GigaChatChatModelExampleV2 {

    public static void main(String[] args) {
        try {
            // Создаем модель с использованием API v2
            GigaChatChatModel model = GigaChatChatModel.builder()
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO_2)
                            // Включаем использование API v2
                            .useV2Completions(true)
                            .reasoningEffort("medium")
                            .disableFilter(true)
                            .repetitionPenalty(1.1f)
                            .build())
                    // URL для API v2 - используйте соответствующий эндпоинт для v2
                    .apiUrl("https://gigachat-ift.sberdevices.delta.sbrf.ru/v2")
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

            System.out.println("Отправляем запрос к GigaChat API v2...");

            // Простой запрос к модели
            var response = model.chat("Привет! Расскажи вкратце о преимуществах API v2 GigaChat.");

            System.out.println("Ответ от модели:");
            System.out.println(response);

        } catch (HttpClientException ex) {
            System.out.println("Ошибка HTTP: код " + ex.statusCode() + ", ответ: " + ex.bodyAsString());
        } catch (Exception ex) {
            System.out.println("Общая ошибка:");
            ex.printStackTrace();
        }
    }
}
