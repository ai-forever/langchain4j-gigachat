package ru.sber.sbe.gigachat.internal.api.model.completions.embed;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Builder(toBuilder = true)
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingDataInnerUsage {
    private Integer promptTokens;
}
