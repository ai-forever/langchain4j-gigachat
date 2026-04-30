package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.http.client.HttpClientException;
import chat.giga.http.client.JdkHttpClientBuilder;
import chat.giga.http.client.SSL;
import chat.giga.model.ModelName;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.Scanner;

public class SmsSenderAgentExample {

    public static void main(String[] args) {

        GigaChatChatModel model = GigaChatChatModel.builder()
                .maxRetries(3)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_PRO_2)
                        .profanityCheck(false)
                        .build())
                .logRequests(true)
                .logResponses(true)
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
                .apiUrl(System.getenv("API_URL"))
                .build();

        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .moderationModel(new DisabledModerationModel())
                .chatMemory(chatMemory)
                .tools(new SMSSender())
                .build();

        Scanner scanner = new Scanner(System.in);
        System.out.println("Начните диалог (введите 'exit' для выхода):");

        try {
            while (true) {
                String userInput = scanner.nextLine();
                if ("exit".equalsIgnoreCase(userInput)) {
                    break;
                }
                System.out.println(assistant.chat(userInput));
            }
            scanner.close();
        } catch (HttpClientException ex) {
            System.out.println("code: " + ex.statusCode() + " response:" + ex.bodyAsString());
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    interface Assistant {

        @SystemMessage("Ты бот для отправки смс. Спроси у пользователя все нужные данные перед отправкой.")
        String chat(@UserMessage String message);
    }

    static class SMSSender {

        @Tool(name = "sendSms", metadata = """
                {
                    "few_shot_examples": [
                    {
                        "request": "Отправь смс на номер",
                        "params" : {
                                       "recipient": "+79683331211",
                                       "message": "Как Дела"
                                   }
                    }
                    ],
                    "return_parameters": {
                            "type": "object",
                            "properties": {
                                "status": {
                                    "type": "string",
                                    "description": "Статус отправки Ok или Error"
                                },
                                "message": {
                                    "type": "integer",
                                    "description": "Детальное сообщение об отправки"
                                }
                            }
                        }
                    }
                """)
        SendSmsResult sendSms(
                @P("Номер телефона получателя") String recipient,
                @P("Содержимое сообщения") String message) {

            System.out.println("отправка сообщения: " + message + " получателю: " + recipient);
            // Здесь должна быть реальная отправка СМС через внешний шлюз
            return new SendSmsResult("OK", "Сообщение отправлено!");
        }

        record SendSmsResult(@Description("Статус отправки сообщения") String status,
                             @Description("Сообщение о результате отправки SMS") String message) {

        }
    }
}
