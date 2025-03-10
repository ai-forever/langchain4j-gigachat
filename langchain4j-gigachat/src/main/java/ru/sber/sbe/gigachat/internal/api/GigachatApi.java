package ru.sber.sbe.gigachat.internal.api;

import retrofit2.Call;
import retrofit2.http.*;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.GigaChatResponse;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedRequest;
import ru.sber.sbe.gigachat.internal.api.model.completions.embed.GigaEmbedResponse;

/**
 * @author protas-nv 16.09.2024
 */
public interface GigachatApi {

    String X_API_KEY = "x-api-key";

    @POST("/v1/chat/completions")
    @Headers({"content-type: application/json"})
    Call<GigaChatResponse> createMessage(@Header(X_API_KEY) String apiKey, @Body GigaChatRequest request);

    @POST("/v1/embeddings")
    @Headers({"content-type: application/json"})
    Call<GigaEmbedResponse> createEmbedding(@Header(X_API_KEY) String apiKey, @Body GigaEmbedRequest request);

}
