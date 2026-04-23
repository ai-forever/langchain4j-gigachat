package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Пример использования GigaChat API v2 с GigaChatStreamingChatModel.
 * <p>
 * В этом примере демонстрируется:
 * <ul>
 *   <li>Потоковое взаимодействие с API v2</li>
 *   <li>Использование асинхронного обработчика ответов</li>
 *   <li>Настройка параметров для потокового режима в v2</li>
 *   <li>Обработка частичных ответов и ошибок</li>
 * </ul>
 *
 * <p><strong>Особенности API v2 в потоковом режиме:</strong>
 * <ul>
 *   <li>Поддержка extended reasoning в потоковом режиме</li>
 *   <li>Улучшенная обработка инструментов (tools) и функций</li>
 *   <li>Расширенные метаданные ответов</li>
 * </ul>
 *
 * @see GigaChatStreamingChatModel
 * @see GigaChatChatRequestParameters
 */
public class GigaChatStreamingChatModelExampleV2 {

    public static void main(String[] args) throws InterruptedException {
        // Используем CountDownLatch для ожидания завершения потока
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<ChatResponse> completeResponse = new AtomicReference<>();

        try {
            // Создаем потоковую модель с использованием API v2
            GigaChatStreamingChatModel model = GigaChatStreamingChatModel.builder()
                    .authClient(AuthClient.builder()
                            .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                    .scope(Scope.GIGACHAT_API_PERS)
                                    .authKey("key")  // Замените на ваш ключ API
                                    .build())
                            .build())
                    .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                            .modelName(ModelName.GIGA_CHAT_PRO_2)
                            // Включаем использование API v2
                            .useV2Completions(true)
                            // Включаем потоковый режим (для потоковой модели это важно)
                            .stream(true)
                            // Настраиваем интервал обновления в потоковом режиме (в секундах)
                            .updateInterval(1)
                            // Настраиваем степень усилия для reasoning-режима
                            .reasoningEffort("medium")
                            // Отключаем фильтрацию контента
                            .disableFilter(true)
                            // Температура для более креативных ответов
                            .temperature(0.8)
                            .build())
                    // URL для API v2 - используйте соответствующий эндпоинт для v2
                    .apiUrl("https://gigachat-ift.sberdevices.delta.sbrf.ru/v2")
                    .verifySslCerts(false)
                    .logRequests(true)
                    .logResponses(true)
                    .build();

            System.out.println("Отправляем потоковый запрос к GigaChat API v2...");
            System.out.println("Ожидаем ответ частями:\n");

            // Отправляем запрос с обработчиком потока
            model.chat("Напиши краткое эссе о будущем искусственного интеллекта (примерно 300 слов). " +
                            "Особое внимание удели этическим аспектам и перспективам развития.",
                    new StreamingChatResponseHandler() {

                        private final StringBuilder fullResponse = new StringBuilder();

                        @Override
                        public void onPartialResponse(String partialText) {
                            // Выводим каждую часть ответа по мере получения
                            System.out.print(partialText);
                            fullResponse.append(partialText);
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse chatResponse) {
                            System.out.println("\n\n=== ПОЛНЫЙ ОТВЕТ ПОЛУЧЕН ===");
                            System.out.println("Общий текст ответа:");
                            System.out.println(fullResponse.toString());

                            System.out.println("\nОтвет модели получен полностью");

                            completeResponse.set(chatResponse);
                            latch.countDown(); // Сигнализируем о завершении
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            System.err.println("\nОШИБКА ПРИ ОБРАБОТКЕ ПОТОКА:");
                            throwable.printStackTrace();
                            latch.countDown(); // Сигнализируем о завершении даже при ошибке
                        }
                    });

            // Ждем завершения потока (максимум 2 минуты)
            boolean completed = latch.await(2, TimeUnit.MINUTES);

            if (!completed) {
                System.out.println("\nТаймаут: потоковый ответ не был завершен за отведенное время.");
            } else if (completeResponse.get() != null) {
                System.out.println("\n\n=== ПРИМЕР УСПЕШНО ЗАВЕРШЕН ===");
            }

        } catch (HttpClientException ex) {
            System.out.println("Ошибка HTTP: код " + ex.statusCode() + ", ответ: " + ex.bodyAsString());
            ex.printStackTrace();
        } catch (Exception ex) {
            System.out.println("Общая ошибка:");
            ex.printStackTrace();
        }

        // Даем время для завершения всех потоков
        TimeUnit.SECONDS.sleep(1);
    }
}
