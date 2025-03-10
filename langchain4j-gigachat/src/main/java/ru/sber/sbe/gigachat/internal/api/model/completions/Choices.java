package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Builder(toBuilder = true)
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class Choices {
    private MessagesRes message;
    private Integer index;
    private FinishReasonEnum finishReason;
}
