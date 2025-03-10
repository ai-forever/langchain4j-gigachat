package ru.sber.sbe.gigachat.internal.api.model.completions;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@AllArgsConstructor
public class GigaChatRequest {
    private ModelEnum model;
    @Builder.Default
    private List<Message> messages = new ArrayList<>();
    private FunctionCallNoneAuto functionCall;

    @Builder.Default
    private List<ChatFunctionsInner> functions = new ArrayList<>();
    private Double temperature;
    private Double topP;
    @Deprecated
    @Builder.Default
    private Long n = 1L;
    @Builder.Default
    private Boolean stream = false;
    private Integer maxTokens;
    private Double repetitionPenalty;

    @Builder.Default
    private BigDecimal updateInterval = BigDecimal.ZERO;

    private Boolean profanityCheck;
}
