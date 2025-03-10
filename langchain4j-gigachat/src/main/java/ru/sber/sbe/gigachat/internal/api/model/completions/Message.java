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
public class Message {
    private RoleEnum role;
    private Object content;
    private String functionsStateId;
    @Builder.Default
    private List<Object> dataForContext = new ArrayList<>();
    private String name;
    private FunctionCallRequest functionCall;
}
