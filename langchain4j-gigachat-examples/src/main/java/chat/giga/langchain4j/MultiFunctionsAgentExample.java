package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.http.client.HttpClientException;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.model.output.structured.Description;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;

import java.util.List;
import java.util.Scanner;

public class MultiFunctionsAgentExample {

    public static void main(String[] args) {

        GigaChatChatModel model = GigaChatChatModel.builder()
                .maxRetries(3)
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(ModelName.GIGA_CHAT_MAX_2)
                        .profanityCheck(false)
                        .build())
                .verifySslCerts(false)
                .logRequests(true)
                .logResponses(true)
                .authClient(AuthClient.builder()
                        .withOAuth(AuthClientBuilder.OAuthBuilder.builder()
                                .scope(Scope.GIGACHAT_API_PERS)
                                .authKey("testkey")
                                .build())
                        .build())
                .build();

        Assistant assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .moderationModel(new DisabledModerationModel())
                .tools(new SearchMovies(), new TripDistance())
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

        String chat(@UserMessage String message);
    }

    static class SearchMovies {

        @Tool("Метод поиска фильмов на основе заданных критериев")
        SearchMoviesResult search(
                @P(value = "Жанр фильма", required = false) String genre,
                @P(value = "Год выпуска фильма", required = false) Integer year,
                @P(value = "Имя актера, снимавшегося в фильме", required = false) String actor) {

            System.out.println("Поиска фильмов  жанр: " + genre + " Год:" + year + " Актер: " + actor);
            return new SearchMoviesResult(List.of("Один дома"));

        }

        record SearchMoviesResult(
                @Description("Список названий фильмов, соответствующих заданным критериям поиска") List<String> movies) {

        }
    }

    static class TripDistance {

        @Tool("Метод расчета расстояние между двумя местоположениями")
        TripDistanceResult calculate(
                @P("Начальное местоположение") String startLocation,
                @P("Конечное местоположение") String endLocation) {

            System.out.println("Расстояние между " + startLocation + " и " + endLocation);
            return new TripDistanceResult(655.0);
        }

        record TripDistanceResult(
                @Description("Расстояние между начальным и конечным местоположением в километрах") Double distance) {

        }
    }
}
