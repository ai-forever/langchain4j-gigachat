package ru.sber.sbe.gigachat.tool;

import dev.langchain4j.agent.tool.JsonSchemaProperty;
import dev.langchain4j.agent.tool.ToolParameters;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.*;

/**
 * @author protas-nv 11.10.2024
 */
@Builder(toBuilder = true)
@Data
@Accessors(fluent = true)
public class GigaChatToolSpecification {
    private final String name;
    private final String description;
    private final ToolParameters parameters;
    private final Map<String, Object> returnParameters;
    private final List<ToolExample> usageExamples;

    public static class GigaChatToolSpecificationBuilder {
        public GigaChatToolSpecificationBuilder addParameter(String name, Iterable<JsonSchemaProperty> jsonSchemaProperties) {
            addOptionalParameter(name, jsonSchemaProperties);
            this.parameters.required().add(name);
            return this;
        }

        public GigaChatToolSpecificationBuilder addOptionalParameter(String name, Iterable<JsonSchemaProperty> jsonSchemaProperties) {
            if (this.parameters == null) {
                this.parameters = ToolParameters.builder().build();
            }

            Map<String, Object> jsonSchemaPropertiesMap = new HashMap<>();
            for (JsonSchemaProperty jsonSchemaProperty : jsonSchemaProperties) {
                jsonSchemaPropertiesMap.put(jsonSchemaProperty.key(), jsonSchemaProperty.value());
            }

            this.parameters.properties().put(name, jsonSchemaPropertiesMap);
            return this;
        }

        public GigaChatToolSpecificationBuilder addReturnParameter(String name, Iterable<JsonSchemaProperty> jsonSchemaProperties) {
            addOptionalReturnParameter(name, jsonSchemaProperties);
//            if (!returnParameters.containsKey("required")) {
//                returnParameters.put("required", new ArrayList<String>());
//            }
//
            return this;
        }

        public GigaChatToolSpecificationBuilder addOptionalReturnParameter(String name, Iterable<JsonSchemaProperty> jsonSchemaProperties) {
            if (this.returnParameters == null) {
                this.returnParameters = new HashMap<>();
            }

            Map<String, Object> jsonSchemaPropertiesMap = new HashMap<>();
            for (JsonSchemaProperty jsonSchemaProperty : jsonSchemaProperties) {
                jsonSchemaPropertiesMap.put(jsonSchemaProperty.key(), jsonSchemaProperty.value());
            }

            this.returnParameters.putAll(jsonSchemaPropertiesMap);
            return this;
        }
    }
}