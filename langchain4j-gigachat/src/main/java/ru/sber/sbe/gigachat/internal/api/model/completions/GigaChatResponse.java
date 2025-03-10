package ru.sber.sbe.gigachat.internal.api.model.completions;

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
public class GigaChatResponse {
    @Builder.Default
    private List<Choices> choices = new ArrayList<>();
    private Long created;
    private String model;
    private Usage usage;
    @JsonProperty("object")
    private String _object;
}
