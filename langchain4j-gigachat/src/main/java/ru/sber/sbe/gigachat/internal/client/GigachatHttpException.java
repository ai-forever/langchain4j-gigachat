package ru.sber.sbe.gigachat.internal.client;

/**
 * @author protas-nv 17.09.2024
 */
public class GigachatHttpException extends RuntimeException {

    public GigachatHttpException(Integer statusCode, String message) {
        super(message);
    }

}
