package chat.giga.langchain4j;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder;
import chat.giga.model.ModelName;
import chat.giga.model.Scope;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.moderation.DisabledModerationModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.tool.ToolProvider;

import java.util.List;

public class MCPServerUsageExample {

    public static void main(String args[]) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:8081/sse")
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

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
                .toolProvider(toolProvider)
                .build();

        assistant.chat("Какая сейчас погода в Москве");
    }

    interface Assistant {

        String chat(@UserMessage String message);
    }
}
