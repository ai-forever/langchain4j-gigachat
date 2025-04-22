package chat.giga.langchain4j.spring;

import chat.giga.client.auth.AuthClient;
import chat.giga.client.auth.AuthClientBuilder.OAuthBuilder;
import chat.giga.client.auth.AuthClientBuilder.UserPasswordAuthBuilder;
import chat.giga.http.client.JdkHttpClient;
import chat.giga.http.client.SSL;
import chat.giga.langchain4j.GigaChatChatModel;
import chat.giga.langchain4j.GigaChatChatRequestParameters;
import chat.giga.langchain4j.GigaChatEmbeddingModel;
import chat.giga.langchain4j.GigaChatImageModel;
import chat.giga.langchain4j.GigaChatStreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import static chat.giga.langchain4j.spring.Properties.PREFIX;

@AutoConfiguration
@EnableConfigurationProperties(Properties.class)
public class AutoConfig {

    @Bean
    @ConditionalOnProperty(PREFIX + ".chat-model.model-name")
    GigaChatChatModel gigaChatChatModel(Properties properties, ObjectProvider<ChatModelListener> listeners) {
        ChatModelProperties chatModelProperties = properties.getChatModel();
        return GigaChatChatModel.builder()
                .logRequests(chatModelProperties.isLogRequests())
                .logResponses(chatModelProperties.isLogResponses())
                .maxRetries(chatModelProperties.getMaxRetries())
                .readTimeout(chatModelProperties.getTimeout())
                .authClient(getAuthClient(chatModelProperties.getAuth()))
                .apiUrl(chatModelProperties.getApiUrl())
                .verifySslCerts(chatModelProperties.isVerifySslCerts())
                .listeners(listeners.orderedStream().toList())
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(chatModelProperties.getModelName())
                        .profanityCheck(chatModelProperties.isProfanityCheck())
                        .temperature(chatModelProperties.getTemperature())
                        .topP(chatModelProperties.getTopP())
                        .frequencyPenalty(chatModelProperties.getFrequencyPenalty())
                        .presencePenalty(chatModelProperties.getPresencePenalty())
                        .maxOutputTokens(chatModelProperties.getMaxTokens())
                        .stopSequences(chatModelProperties.getStop())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".streaming-chat-model.model-name")
    GigaChatStreamingChatModel streamingGigaChatChatModel(Properties properties,
            ObjectProvider<ChatModelListener> listeners) {
        ChatModelProperties chatModelProperties = properties.getStreamingChatModel();
        return GigaChatStreamingChatModel.builder()
                .logRequests(chatModelProperties.isLogRequests())
                .logResponses(chatModelProperties.isLogResponses())
                .readTimeout(chatModelProperties.getTimeout())
                .authClient(getAuthClient(chatModelProperties.getAuth()))
                .apiUrl(chatModelProperties.getApiUrl())
                .verifySslCerts(chatModelProperties.isVerifySslCerts())
                .listeners(listeners.orderedStream().toList())
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(chatModelProperties.getModelName())
                        .profanityCheck(chatModelProperties.isProfanityCheck())
                        .temperature(chatModelProperties.getTemperature())
                        .topP(chatModelProperties.getTopP())
                        .frequencyPenalty(chatModelProperties.getFrequencyPenalty())
                        .presencePenalty(chatModelProperties.getPresencePenalty())
                        .maxOutputTokens(chatModelProperties.getMaxTokens())
                        .stopSequences(chatModelProperties.getStop())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".embedding-model.model-name")
    GigaChatEmbeddingModel embeddingGigaChatChatModel(Properties properties) {
        EmbeddingModelProperties chatModelProperties = properties.getEmbeddingModel();
        return GigaChatEmbeddingModel.builder()
                .logRequests(chatModelProperties.isLogRequests())
                .logResponses(chatModelProperties.isLogResponses())
                .readTimeout(chatModelProperties.getTimeout())
                .authClient(getAuthClient(chatModelProperties.getAuth()))
                .apiUrl(chatModelProperties.getApiUrl())
                .verifySslCerts(chatModelProperties.isVerifySslCerts())
                .modelName(chatModelProperties.getModelName())
                .batchSize(properties.getEmbeddingModel().getBatchSize())
                .build();
    }

    @Bean
    @ConditionalOnProperty(PREFIX + ".image-model.model-name")
    GigaChatImageModel gigaChatImageModel(Properties properties, ObjectProvider<ChatModelListener> listeners) {
        ChatModelProperties chatModelProperties = properties.getImageModel();
        return GigaChatImageModel.builder()
                .logRequests(chatModelProperties.isLogRequests())
                .logResponses(chatModelProperties.isLogResponses())
                .maxRetries(chatModelProperties.getMaxRetries())
                .readTimeout(chatModelProperties.getTimeout())
                .authClient(getAuthClient(chatModelProperties.getAuth()))
                .apiUrl(chatModelProperties.getApiUrl())
                .verifySslCerts(chatModelProperties.isVerifySslCerts())
                .listeners(listeners.orderedStream().toList())
                .defaultChatRequestParameters(GigaChatChatRequestParameters.builder()
                        .modelName(chatModelProperties.getModelName())
                        .profanityCheck(chatModelProperties.isProfanityCheck())
                        .temperature(chatModelProperties.getTemperature())
                        .topP(chatModelProperties.getTopP())
                        .frequencyPenalty(chatModelProperties.getFrequencyPenalty())
                        .presencePenalty(chatModelProperties.getPresencePenalty())
                        .maxOutputTokens(chatModelProperties.getMaxTokens())
                        .stopSequences(chatModelProperties.getStop())
                        .build())
                .build();
    }

    private AuthClient getAuthClient(AuthProperties auth) {
        return switch (auth.getType()) {
            case OAUH -> AuthClient.builder().withOAuth(OAuthBuilder.builder()
                    .authKey(auth.getAuthKey())
                    .clientId(auth.getClientId())
                    .scope(auth.getScope())
                    .clientSecret(auth.getClientSecret())
                    .authApiUrl(auth.getAuthApiUrl())
                    .verifySslCerts(auth.isVerifySslCerts())
                    .readTimeout(auth.getTimeout())
                    .build()).build();
            case USER_PASS -> AuthClient.builder().withUserPassword(UserPasswordAuthBuilder.builder()
                            .authApiUrl(auth.getAuthApiUrl())
                            .verifySslCerts(auth.isVerifySslCerts())
                            .scope(auth.getScope())
                            .readTimeout(auth.getTimeout())
                            .password(auth.getPassword())
                            .user(auth.getUser())
                            .verifySslCerts(auth.isVerifySslCerts())
                            .build())
                    .build();
            case ACCESS_TOKEN -> AuthClient.builder()
                    .withProvidedTokenAuth(auth.getAccessToken())
                    .build();
            case CERTIFICATES -> AuthClient.builder().withCertificatesAuth(JdkHttpClient.builder()
                    .ssl(SSL.builder()
                            .truststorePassword(auth.getTrustStorePassword())
                            .trustStoreType(auth.getTrustStoreType())
                            .truststorePath(auth.getTrustStorePath())
                            .keystorePassword(auth.getKeyStorePassword())
                            .keystoreType(auth.getKeyStoreType())
                            .keystorePath(auth.getKeyStorePath())
                            .build())
                    .build()).build();
        };

    }
}
