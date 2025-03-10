package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class ChatFunctionsInner {
    private String name;
    private String description;
    private Object parameters;
    private List<ChatFunctionsInnerFewShotExamplesInner> fewShotExamples = new ArrayList<>();
    private Object returnParameters;
}
