package ru.sber.sbe.gigachat.usage;

import dev.langchain4j.model.output.TokenUsage;

import static dev.langchain4j.internal.Utils.getOrDefault;

/**
 * @author protas-nv 13.02.2025
 */
public class GigaUsage extends TokenUsage {
    private final Integer precachedPromptTokenCount;

    public GigaUsage(Integer precachedPromptTokenCount) {
        this.precachedPromptTokenCount = precachedPromptTokenCount;
    }

    public GigaUsage(Integer inputTokenCount, Integer precachedPromptTokenCount) {
        super(inputTokenCount);
        this.precachedPromptTokenCount = precachedPromptTokenCount;
    }

    public GigaUsage(Integer inputTokenCount, Integer outputTokenCount, Integer precachedPromptTokenCount) {
        super(inputTokenCount, outputTokenCount, sum(sum(inputTokenCount, outputTokenCount), precachedPromptTokenCount));
        this.precachedPromptTokenCount = precachedPromptTokenCount;
    }

    public static Integer sum(Integer first, Integer second) {
        if (first == null && second == null) {
            return null;
        }

        return getOrDefault(first, 0) + getOrDefault(second, 0);
    }

    public GigaUsage(Integer inputTokenCount, Integer outputTokenCount, Integer precached, Integer totalTokenCount) {
        super(inputTokenCount, outputTokenCount, totalTokenCount);
        this.precachedPromptTokenCount = precached;
    }

    public String toString() {
        return "GigaUsage{super=" + super.toString() + ", precachedPromptTokenCount=" +
               this.getPrecachedPromptTokenCount() + "}";
    }

    public Integer getPrecachedPromptTokenCount() {
        return this.precachedPromptTokenCount;
    }
}