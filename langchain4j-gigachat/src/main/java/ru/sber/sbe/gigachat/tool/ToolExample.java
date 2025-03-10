package ru.sber.sbe.gigachat.tool;

import lombok.*;

import java.util.Map;

/**
 * @author protas-nv 11.10.2024
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolExample {
    String requestExample;
    Map<String, Object> params;
}
