package ru.sber.sbe.gigachat.internal.client;

import dev.langchain4j.spi.ServiceHelper;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatResponse;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedResponse;

import java.time.Duration;

/**
 * @author protas-nv 17.09.2024
 */
public abstract class GigachatClient {
    @SuppressWarnings("rawtypes")
    public static GigachatClient.Builder builder() {
        for (GigachatClientBuilderFactory factory : ServiceHelper.loadFactories(GigachatClientBuilderFactory.class)) {
            return factory.get();
        }
        // fallback to the default
        return DefaultGigachatClient.builder();
    }

    public abstract GigaChatResponse createMessage(GigaChatRequest request);

    public abstract GigaEmbedResponse createEmbedding(GigaEmbedRequest request);

    public abstract static class Builder<T extends GigachatClient, B extends Builder<T, B>> {
        public String baseUrl;
        public String apiKey;
        public Duration timeout;
        public Boolean logRequests;
        public Boolean logResponses;
        public String certFilePath;
        public String keyFilePath;
        public String caBundleFile;

        public abstract T build();

        public B certFilePath(String certFilePath) {
//            if ((certFilePath == null) || certFilePath.trim().isEmpty()) {
//                throw new IllegalArgumentException("baseUrl cannot be null or empty");
//            }
            this.certFilePath = certFilePath;
            return (B) this;
        }

        public B keyFilePath(String keyFilePath) {
//            if ((keyFilePath == null) || keyFilePath.trim().isEmpty()) {
//                throw new IllegalArgumentException("keyFilePath cannot be null or empty");
//            }
            this.keyFilePath = keyFilePath;
            return (B) this;
        }

        public B caBundleFile(String caBundleFile) {
//            if ((caBundleFile == null) || caBundleFile.trim().isEmpty()) {
//                throw new IllegalArgumentException("caBundleFile cannot be null or empty");
//            }
            this.caBundleFile = caBundleFile;
            return (B) this;
        }

        public B baseUrl(String baseUrl) {
            if ((baseUrl == null) || baseUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("baseUrl cannot be null or empty");
            }
            this.baseUrl = baseUrl;
            return (B) this;
        }

        public B apiKey(String apiKey) {
            this.apiKey = apiKey;
            return (B) this;
        }

        public B timeout(Duration timeout) {
            if (timeout == null) {
                throw new IllegalArgumentException("timeout cannot be null");
            }
            this.timeout = timeout;
            return (B) this;
        }

        public B logRequests() {
            return logRequests(true);
        }

        public B logRequests(Boolean logRequests) {
            if (logRequests == null) {
                logRequests = false;
            }
            this.logRequests = logRequests;
            return (B) this;
        }

        public B logResponses() {
            return logResponses(true);
        }

        public B logResponses(Boolean logResponses) {
            if (logResponses == null) {
                logResponses = false;
            }
            this.logResponses = logResponses;
            return (B) this;
        }
    }
}