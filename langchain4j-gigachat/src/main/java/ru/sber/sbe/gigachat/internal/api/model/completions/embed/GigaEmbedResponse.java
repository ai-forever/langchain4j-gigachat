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
public class GigaEmbedResponse {

    @JsonProperty("object")
    @Builder.Default
    private String _object = "list";

    @Builder.Default
    private List<EmbeddingDataInner> data = new ArrayList<>();

    @Builder.Default
    private String model = "Embeddings";

}
