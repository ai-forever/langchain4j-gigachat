package ru.sber.sbe.gigachat.internal.api.model.completions.embed;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class EmbeddingDataInner {
    @JsonProperty("object")
    @Builder.Default
    private String _object = "embedding";
    @Builder.Default
    private List<Float> embedding = new ArrayList<>();

    private Integer index;

    private EmbeddingDataInnerUsage usage;
}
