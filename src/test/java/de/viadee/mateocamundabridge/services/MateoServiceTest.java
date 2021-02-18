package de.viadee.mateocamundabridge.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.ExternalTaskBusinessError;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.ProcessConstants;
import de.viadee.mateocamundabridge.dtos.OrchestratorJobDTO;
import de.viadee.mateocamundabridge.dtos.ReportDTO;
import de.viadee.mateocamundabridge.exceptions.MateoBridgeRuntimeException;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * @author Marcel_Flasskamp
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig
@SpringBootTest
@ContextConfiguration(classes = { MateoService.class, MateoApiProperties.class,
        MateoServiceTest.ContextConfiguration.class })
class MateoServiceTest {

    @Autowired
    private MateoService mateoService;

    @Autowired
    private MateoApiProperties mateoApiProperties;

    private String url = "http://localhost:8080";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void init() throws MalformedURLException {
        mateoApiProperties.setUrl(url);
    }

    @Test
    void testGetStorageVariables_OneVariable() {
        // given
        List<String> scriptVariables = new ArrayList<>();
        scriptVariables.add("varKey");
        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("varKey", "varValue");
        String requestUrl = url + "/api/storage/get?key=varKey";

        //when
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrl), eq(String.class)))
                .thenReturn(new ResponseEntity<>("varValue", HttpStatus.OK));
        Map<String, Object> response = mateoService.getStorageVariables(scriptVariables);

        //then
        assertEquals(expectedMap, response);
    }

    @Test
    void testGetStorageVariables_MoreVariable() {
        // given
        List<String> scriptVariables = new ArrayList<>();
        scriptVariables.add("oneKey");
        scriptVariables.add("twoKey");
        scriptVariables.add("threeKey");
        HashMap<String, Object> expectedMap = new HashMap<>();
        expectedMap.put("oneKey", "oneValue");
        expectedMap.put("twoKey", "twoValue");
        expectedMap.put("threeKey", "threeValue");
        String requestUrlOne = url + "/api/storage/get?key=" + scriptVariables.get(0);
        String requestUrlTwo = url + "/api/storage/get?key=" + scriptVariables.get(1);
        String requestUrlThree = url + "/api/storage/get?key=" + scriptVariables.get(2);

        //when
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlOne), eq(String.class)))
                .thenReturn(new ResponseEntity<>("oneValue", HttpStatus.OK));
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlTwo), eq(String.class)))
                .thenReturn(new ResponseEntity<>("twoValue", HttpStatus.OK));
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlThree), eq(String.class)))
                .thenReturn(new ResponseEntity<>("threeValue", HttpStatus.OK));
        Map<String, Object> response = mateoService.getStorageVariables(scriptVariables);

        //then
        assertEquals(expectedMap, response);
    }

    @Test
    void testGetStorageVariables_BadRequest() {
        // given
        List<String> scriptVariables = new ArrayList<>();
        scriptVariables.add("oKey");
        scriptVariables.add("tKey");
        scriptVariables.add("thKey");

        String requestUrlOne = url + "/api/storage/get?key=" + scriptVariables.get(0);
        String requestUrlTwo = url + "/api/storage/get?key=" + scriptVariables.get(1);
        String requestUrlThree = url + "/api/storage/get?key=" + scriptVariables.get(2);

        //when
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlOne), eq(String.class)))
                .thenReturn(new ResponseEntity<>("oneValue", HttpStatus.OK));
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlTwo), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Error while modifying storage", HttpStatus.BAD_REQUEST));
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrlThree), eq(String.class)))
                .thenReturn(new ResponseEntity<>("threeValue", HttpStatus.OK));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoService.getStorageVariables(scriptVariables));

        //then
        assertEquals("Some variables couldn't be read from mateo", exception.getMessage());
    }

    @Test
    void testGetStorageVariables_NotExistingVariable() {
        // given
        List<String> scriptVariables = new ArrayList<>();
        scriptVariables.add("notExisting");

        String requestUrl = url + "/api/storage/get?key=notExisting";

        //when
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrl), eq(String.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoService.getStorageVariables(scriptVariables));

        //then
        assertEquals("Some variables couldn't be read from mateo", exception.getMessage());
    }

    @Test
    void testGetStorageVariables_NotExistingVariableNoContent() {
        // given
        List<String> scriptVariables = new ArrayList<>();
        scriptVariables.add("noContent");

        String requestUrl = url + "/api/storage/get?key=" + scriptVariables.get(0);

        //when
        when(mateoService.getRestTemplate().getForEntity(eq(requestUrl), eq(String.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoService.getStorageVariables(scriptVariables));

        //then
        assertEquals("Some variables couldn't be read from mateo", exception.getMessage());
    }

    @Test
    void testSetStorageVariables_BadRequest() {
        //given
        Map<String, String> scriptVariables = new HashMap<>();
        scriptVariables.put("someKey", "sValue");
        String expectedUrl = url + "/api/storage";

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(expectedUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("sValue", HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoService.setStorageVariables(scriptVariables));

        // then
        assertEquals("Variables could not be written to the storage file.",
                exception.getMessage());
    }

    @Test
    void testSetStorageVariables_correctOneVariable() {
        //given
        Map<String, String> scriptVariables = new HashMap<>();
        scriptVariables.put("sKey", "sValue");
        String expectedUrl = url + "/api/storage";

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(scriptVariables, headers);

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(expectedUrl), eq(entity), eq(String.class)))
                .thenReturn(new ResponseEntity<>("sVariable", HttpStatus.OK));

        mateoService.setStorageVariables(scriptVariables);

        // then
        verify(mateoService.getRestTemplate(), times(1))
                .postForEntity(eq(expectedUrl), eq(entity), eq(String.class));
    }

    @Test
    void testSetStorageVariables_correctMoreVariable() {
        //given
        Map<String, String> scriptVariables = new HashMap<>();
        scriptVariables.put("oneKey", "oneValue");
        scriptVariables.put("twoKey", "twoValue");
        scriptVariables.put("threeKey", "threeValue");
        String requestUrl = url + "/api/storage";

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(scriptVariables, headers);

        //when
        when(mateoService.getRestTemplate().postForEntity(eq(requestUrl), eq(entity), eq(String.class)))
                .thenReturn(new ResponseEntity<>("{\"oneKey\":\"oneValue\",\"twoKey\":\"twoValue\",\"threeKey\":\"threeValue\"}", HttpStatus.OK));

        mateoService.setStorageVariables(scriptVariables);

        // then
        verify(mateoService.getRestTemplate(), times(1))
                .postForEntity(eq(requestUrl), eq(entity), eq(String.class));

    }

    @Test
    void testStartScript() throws JsonProcessingException {
        //given
        ReportDTO expectedDTO = new ReportDTO();
        String scriptfile = "correct";
        String requestUrl = url + "/api/execute/script-files?filename=" + scriptfile;

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(requestUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(objectMapper.writeValueAsString(expectedDTO), HttpStatus.OK));

        ReportDTO resultDto = mateoService.startScript(scriptfile);

        //then
        assertEquals(expectedDTO.toString(), resultDto.toString());
    }

    @Test
    void testStartScript_Abort() {
        //given
        String scriptfile = "abort";
        String requestUrl = url + "/api/execute/script-files?filename=" + scriptfile;

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(requestUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Aborted", HttpStatus.OK));

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoService.startScript(scriptfile));

        //then
        assertEquals("Script execution aborted", exception.getMessage());
    }

    @Test
    void testStartScript_INTERNAL_SERVER_ERROR() {
        //given
        String scriptfile = "INTERNAL_SERVER_ERROR";
        String requestUrl = url + "/api/execute/script-files?filename=" + scriptfile;

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(requestUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Some Exception Message", HttpStatus.INTERNAL_SERVER_ERROR));

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoService.startScript(scriptfile));

        //then
        assertEquals("Some Exception Message", exception.getMessage());
    }

    @Test
    void testStartScript_WrongResponseDto() throws JsonProcessingException {
        //given
        OrchestratorJobDTO wrongDto = new OrchestratorJobDTO();
        String scriptfile = "wrongDto";
        String requestUrl = url + "/api/execute/script-files?filename=" + scriptfile;

        //when
        when(mateoService.getRestTemplate()
                .postForEntity(eq(requestUrl), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(objectMapper.writeValueAsString(wrongDto), HttpStatus.OK));

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoService.startScript(scriptfile));

        //then
        assertEquals("Couldn't parse answer from mateo", exception.getMessage());
    }

    @Test
    void testIsMateoOnline_Online() throws MalformedURLException {
        mateoService.mateoApiProperties.setUrl("http://isOnline.de/");
        when(mateoService.getRestTemplate().getForEntity(eq("http://isOnline.de/api/status"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Up", HttpStatus.OK));
        boolean isOnline = mateoService.isMateoOnline();

        assertTrue(isOnline);
    }

    @Test
    void testIsMateoOnline_Offline() {
        when(mateoService.getRestTemplate().getForEntity(anyString(), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Up", HttpStatus.BAD_REQUEST));
        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoService.isMateoOnline());

        assertEquals("Mateo is offline", exception.getMessage());
    }

    @Test
    void testStartScriptWithVariables_Correct() throws JsonProcessingException {
        //given
        String scriptName = "scriptNameDummy";
        Map<String, Object> inputParams = new HashMap<>();
        List<String> outputParams = new ArrayList<>();
        ReportDTO expectedReportDTO = new ReportDTO();
        expectedReportDTO.setResultString("Erfolg");

        //when
        when(mateoService.getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(objectMapper.writeValueAsString(expectedReportDTO), HttpStatus.OK));

        ReportDTO resultDto = mateoService.startScriptWithVariables(scriptName, inputParams, outputParams);

        //then
        assertEquals(expectedReportDTO.getResultString(), resultDto.getResultString());
        assertEquals(expectedReportDTO.getResultString(),
                resultDto.getOutputVariables().get(ProcessConstants.EXTOUT_MATEO_SCRIPT_RESULT));
    }

    @Test
    void testStartScriptWithVariables_ExceptionScriptresultNoRun() throws JsonProcessingException {
        //given
        String scriptName = "scriptNameDummy";
        Map<String, Object> inputParams = new HashMap<>();
        List<String> outputParams = new ArrayList<>();
        ReportDTO expectedReportDTO = new ReportDTO();
        expectedReportDTO.setResultString("NoRun");

        //when
        when(mateoService.getRestTemplate().postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(objectMapper.writeValueAsString(expectedReportDTO), HttpStatus.OK));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoService.startScriptWithVariables(scriptName, inputParams, outputParams));

        //then
        assertEquals("Script run wasn't successful", exception.getMessage());
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    @Configuration
    static class ContextConfiguration {

        @Bean
        public RestTemplateBuilder restTemplateBuilder() {

            RestTemplateBuilder rtb = mock(RestTemplateBuilder.class);
            RestTemplate restTemplate = mock(RestTemplate.class);

            when(rtb.build()).thenReturn(restTemplate);
            return rtb;
        }
    }
}