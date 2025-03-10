package ru.sber.sbe.gigachat.sanitizer;

import dev.langchain4j.data.message.ChatMessage;

import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.internal.ValidationUtils.ensureNotEmpty;

/**
 * @author protas-nv 17.09.2024
 */
public class MessageSanitizer {

    public static List<ChatMessage> sanitizeMessages(List<ChatMessage> messages) {
        ensureNotEmpty(messages, "messages");
        // just do nothing for now
        return new ArrayList<>(messages);
    }
}
