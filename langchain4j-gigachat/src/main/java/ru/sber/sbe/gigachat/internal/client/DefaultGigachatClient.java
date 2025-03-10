package ru.sber.sbe.gigachat.internal.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.Utils;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.pem.util.PemUtils;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import org.openapitools.jackson.nullable.JsonNullableModule;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import ru.sber.sbe.gigachat.internal.api.GigachatApi;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatResponse;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedResponse;

import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.IOException;
import java.nio.file.Paths;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static dev.langchain4j.internal.Utils.isNotNullOrBlank;
import static dev.langchain4j.internal.ValidationUtils.ensureNotBlank;

/**
 * @author protas-nv 17.09.2024
 */
public class DefaultGigachatClient extends GigachatClient {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(INDENT_OUTPUT)
            .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(new JsonNullableModule());

    private final GigachatApi gigachatApi;
    private final OkHttpClient okHttpClient;

    private final String apiKey;
    private final boolean logResponses;

    DefaultGigachatClient(Builder builder) {
        this.apiKey = builder.apiKey;
        this.logResponses = builder.logResponses;

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder()
                .callTimeout(builder.timeout)
                .connectTimeout(builder.timeout)
                .readTimeout(builder.timeout)
                .writeTimeout(builder.timeout);

        if (isNotNullOrBlank(builder.caBundleFile) || isNotNullOrBlank(builder.certFilePath) ||
            isNotNullOrBlank(builder.keyFilePath)) {
            X509ExtendedKeyManager keyManager =
                    PemUtils.loadIdentityMaterial(Paths.get(builder.certFilePath), Paths.get(builder.keyFilePath));
            X509ExtendedTrustManager trustManager =
                    PemUtils.loadTrustMaterial(Paths.get(builder.caBundleFile));

            SSLFactory sslFactory = SSLFactory.builder()
                                              .withIdentityMaterial(keyManager)
                                              .withTrustMaterial(trustManager)
                                              .build();

            okHttpClientBuilder.sslSocketFactory(sslFactory.getSslSocketFactory(), sslFactory.getTrustManager().get());
        }

        if (builder.logRequests) {
            okHttpClientBuilder.addInterceptor(new GigachatRequestLoggingInterceptor());
        }

        if (logResponses) {
            okHttpClientBuilder.addInterceptor(new GigachatResponseLoggingInterceptor());
        }

        this.okHttpClient = okHttpClientBuilder.build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Utils.ensureTrailingForwardSlash(ensureNotBlank(builder.baseUrl, "baseUrl")))
                .client(okHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(OBJECT_MAPPER))
                .build();

        this.gigachatApi = retrofit.create(GigachatApi.class);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public GigaChatResponse createMessage(GigaChatRequest request) {
        try {
            retrofit2.Response<GigaChatResponse> retrofitResponse =
                    gigachatApi.createMessage(apiKey, request).execute();
            if (retrofitResponse.isSuccessful()) {
                return retrofitResponse.body();
            } else {
                try (ResponseBody errorBody = retrofitResponse.errorBody()) {
                    if (errorBody != null) {
                        throw new GigachatHttpException(retrofitResponse.code(), errorBody.string());
                    }
                }
                throw new GigachatHttpException(retrofitResponse.code(), null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GigaEmbedResponse createEmbedding(GigaEmbedRequest request) {
        try {
            retrofit2.Response<GigaEmbedResponse> retrofitResponse =
                    gigachatApi.createEmbedding(apiKey, request).execute();
            if (retrofitResponse.isSuccessful()) {
                return retrofitResponse.body();
            } else {
                try (ResponseBody errorBody = retrofitResponse.errorBody()) {
                    if (errorBody != null) {
                        throw new GigachatHttpException(retrofitResponse.code(), errorBody.string());
                    }
                }
                throw new GigachatHttpException(retrofitResponse.code(), null);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Builder extends GigachatClient.Builder<DefaultGigachatClient, Builder> {

        public DefaultGigachatClient build() {
            return new DefaultGigachatClient(this);
        }
    }
}