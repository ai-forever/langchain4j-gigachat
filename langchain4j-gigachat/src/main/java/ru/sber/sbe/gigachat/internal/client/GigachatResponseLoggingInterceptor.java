package ru.sber.sbe.gigachat.internal.client;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static ru.sber.sbe.gigachat.Utils.minifyJson;
import static ru.sber.sbe.gigachat.internal.client.GigachatRequestLoggingInterceptor.getHeaders;

/**
 * @author protas-nv 17.09.2024
 */

@Slf4j
public class GigachatResponseLoggingInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        this.log(response);
        return response;
    }

    private void log(Response response) {
        try {
            log.debug("Response:\n- status code: {}\n- headers: {}\n- body: {}",
                    response.code(), getHeaders(response.headers()), this.getBody(response));
        } catch (Exception e) {
            log.warn("Error while logging response: {}", e.getMessage());
        }
    }

    private String getBody(Response response) throws IOException {
        return isEventStream(response)
                ? "[skipping response body due to streaming]"
                : minifyJson(response.peekBody(Long.MAX_VALUE).string());
    }

    private static boolean isEventStream(Response response) {
        String contentType = response.header("Content-Type");
        return contentType != null && contentType.contains("event-stream");
    }
}
