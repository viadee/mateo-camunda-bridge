package de.viadee.mateocamundabridge.utils;

import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.impl.VariableMapImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marcel_Flasskamp
 */
class VariableConverterTest {

    @Test
    void testToMapStringObject() {
        // given
        Date date = new Date();
        VariableMap variableMap = new VariableMapImpl();
        variableMap.putIfAbsent("key", "value");
        variableMap.putIfAbsent("date", date);

        // when
        Map<String, String> mapResult = VariableConverter.toMapStringObject(variableMap);

        // then
        assertEquals("value", mapResult.get("key"));
        assertEquals(date.toString(), mapResult.get("date"));
    }

    @Test
    void testToEngineValuesTest() {
        // given
        Date date = new Date();
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        properties.put("date", date);

        // when
        Map<String, Object> mapResult = VariableConverter.toEngineValues(properties);

        // then
        assertEquals("value", mapResult.get("key"));
        assertEquals(Date.from(
                ZonedDateTime.parse(date.toInstant().toString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()),
                mapResult.get("date"));
    }

    @Test
    void testToMapStringString() {
        // given
        LocalDate date = LocalDate.parse("2014-02-14");
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");
        properties.put("date", date);

        // when
        Map<String, String> mapResult = VariableConverter.toMapStringString(properties);

        // then
        assertEquals("value", mapResult.get("key"));
        assertEquals("2014-02-14", mapResult.get("date"));
    }
}