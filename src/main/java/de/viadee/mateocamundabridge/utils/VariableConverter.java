package de.viadee.mateocamundabridge.utils;

import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.value.DateValue;
import org.camunda.bpm.engine.variable.value.ObjectValue;
import org.camunda.bpm.engine.variable.value.TypedValue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Utility class around variable conversion between engine and external service
 */
public class VariableConverter {

    private VariableConverter() {
    }

    /**
     * Convert a VariableMap (from camunda externalTask) to Map<String, String>
     *
     * @param variableMap variableMap from camunda
     * @return Map with key, value from variable Map
     */
    public static Map<String, String> toMapStringObject(VariableMap variableMap) {
        Map<String, String> variablesSafe = new HashMap<>();
        for (String variableKey : variableMap.keySet()) {
            TypedValue valueTyped = variableMap.getValueTyped(variableKey);
            String value;
            if (valueTyped instanceof DateValue) {
                value = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime
                        .ofInstant(((DateValue) valueTyped).getValue().toInstant(), ZoneId.systemDefault()));
            } else if (valueTyped instanceof ObjectValue) {
                value = ((ObjectValue) valueTyped).getValueSerialized();
            } else {
                value = valueTyped.getValue() != null ? valueTyped.getValue().toString() : "";
            }
            variablesSafe.put(variableKey, value);
        }
        return variablesSafe;
    }

    public static Map<String, String> toMapStringString(Map<String, Object> variableMap) {
        if (variableMap == null || variableMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> variablesSafe = new HashMap<>();
        for (Entry<String, Object> entry : variableMap.entrySet()) {
            String value;
            if (entry.getValue() instanceof DateValue) {
                value = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(ZonedDateTime
                        .ofInstant(((DateValue) entry.getValue()).getValue().toInstant(), ZoneId.systemDefault()));
            } else if (entry.getValue() instanceof ObjectValue) {
                value = ((ObjectValue) entry.getValue()).getValueSerialized();
            } else {
                value = entry.getValue() != null ? entry.getValue().toString() : "";
            }
            variablesSafe.put(entry.getKey(), value);
        }
        return variablesSafe;
    }

    /**
     * Inspects a map of values and transforms them if necessary. For
     * {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}-formatted String values, a
     * {@link Date} will be created.
     *
     * @param properties the map of values to inspect
     * @return Map of String, Object for the camunda engine
     */
    public static Map<String, Object> toEngineValues(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = new HashMap<>();
        for (Entry<String, Object> property : properties.entrySet()) {
            Object value = property.getValue();
            if (value instanceof String) {
                try {
                    value = Date.from(ZonedDateTime
                            .parse((String) property.getValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
                } catch (DateTimeParseException e) {
                    // do nothing
                }
            }
            variables.put(property.getKey(), value);
        }
        return variables;
    }

    public static Map<String, Object> toEngineValuesFromStringString(Map<String, String> properties) {
        if (properties == null || properties.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> variables = new HashMap<>();
        for (Entry<String, String> property : properties.entrySet()) {
            Object value  = property.getValue();
                try {
                    value = Date.from(ZonedDateTime
                            .parse(property.getValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant());
                } catch (DateTimeParseException e) {
                    // do nothing
                }
            variables.put(property.getKey(), value);
        }
        return variables;
    }
}
