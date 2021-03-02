package de.viadee.mateocamundabridge.services;

import de.viadee.bpm.camunda.externaltask.retry.aspect.error.ExternalTaskBusinessError;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.Constants;
import de.viadee.mateocamundabridge.dtos.CamundaOrchestratorJob;
import de.viadee.mateocamundabridge.dtos.OrchestratorJobDTO;
import de.viadee.mateocamundabridge.exceptions.MateoBridgeRuntimeException;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringJUnitConfig
@SpringBootTest
@ActiveProfiles("test-orchestrator")
@ContextConfiguration(classes = {  MateoApiProperties.class, MateoOrchestratorService.class,
        MateoOrchestratorServiceTest.ContextConfiguration.class })
class MateoOrchestratorServiceTest {

    @Autowired
    private MateoOrchestratorService mateoOrchestratorService;

    private final ExternalTask externalTask = mock(ExternalTask.class, Mockito.RETURNS_DEEP_STUBS);

    private final ExternalTaskService externalTaskService = mock(ExternalTaskService.class, Mockito.RETURNS_DEEP_STUBS);

    @BeforeEach
    public void init() {
        mateoOrchestratorService.startedJobs.clear();
    }

    @Test
    void testValidateAndCompleteJob_Count() {
        //given
        OrchestratorJobDTO jobDTO = new OrchestratorJobDTO();
        jobDTO.setUuid("testCompleteTask_Count");
        jobDTO.setStatus(Constants.JOB_STATUS_RUNNING);
        CamundaOrchestratorJob camundaOrchestratorJob = new CamundaOrchestratorJob(jobDTO.getUuid(),
                externalTaskService, externalTask);
        mateoOrchestratorService.startedJobs.add(camundaOrchestratorJob);

        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://localhost:8080/api/job?uuid=" + jobDTO.getUuid()),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(jobDTO, HttpStatus.OK));

        mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob);
        mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob);
        mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob);

        assertEquals(3, mateoOrchestratorService.startedJobs.get(0).getAskedTimes());
    }

    @Test
    void testValidateAndCompleteJob_Failed() {
        OrchestratorJobDTO jobDTO = new OrchestratorJobDTO();
        jobDTO.setUuid("testCompleteTask_OrchestratorException");
        jobDTO.setStatus(Constants.JOB_STATUS_FAILED);
        CamundaOrchestratorJob camundaOrchestratorJob = new CamundaOrchestratorJob(jobDTO.getUuid(),
                externalTaskService, externalTask);
        mateoOrchestratorService.startedJobs.add(camundaOrchestratorJob);

        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://localhost:8080/api/job?uuid=" + jobDTO.getUuid()),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(jobDTO, HttpStatus.OK));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob));

        assertEquals("Job failed", exception.getMessage()); // Exception was thrown
        verify(externalTaskService, times(0)).complete(any(), anyMap()); // Task not completed
        assertEquals(0, mateoOrchestratorService.startedJobs.size()); // Job was removed
    }

    @Test
    void testValidateAndCompleteJob_NoResponse() {
        String uuid = "testCompleteTask_NoResponse";
        CamundaOrchestratorJob camundaOrchestratorJob = new CamundaOrchestratorJob(uuid, externalTaskService,
                externalTask);
        mateoOrchestratorService.startedJobs.add(camundaOrchestratorJob);

        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://localhost:8080/api/job?uuid=" + uuid), eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob));

        assertEquals("Couldn't find Job", exception.getMessage()); //Exception was thrown
        assertEquals(0, mateoOrchestratorService.startedJobs.size()); // Job was removed
        verify(externalTaskService, times(0)).complete(any(), anyMap()); // Task not completed
    }

    @Test
    void testValidateAndCompleteJob_Correct() {
        //given
        OrchestratorJobDTO jobDTO = new OrchestratorJobDTO();
        jobDTO.setResultString("Erfolg");
        jobDTO.setUuid("testCompleteTask_Correct");
        jobDTO.setStatus(Constants.JOB_STATUS_FINISHED);
        CamundaOrchestratorJob camundaOrchestratorJob = new CamundaOrchestratorJob(jobDTO.getUuid(),
                externalTaskService, externalTask);
        mateoOrchestratorService.startedJobs.add(camundaOrchestratorJob);

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://localhost:8080/api/job?uuid=" + jobDTO.getUuid()),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(jobDTO, HttpStatus.OK));

        mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob);

        //then
        assertEquals(0, mateoOrchestratorService.startedJobs.size()); // Job completed
        verify(externalTaskService, times(1)).complete(eq(externalTask), anyMap()); // completed
    }

    @Test
    void testValidateAndCompleteJob_ScriptAbort() {
        //given
        UUID uuid = UUID.randomUUID();
        OrchestratorJobDTO jobDTO = new OrchestratorJobDTO();
        jobDTO.setResultString("Abbruch");
        jobDTO.setUuid(uuid.toString());
        jobDTO.setStatus(Constants.JOB_STATUS_FINISHED);
        CamundaOrchestratorJob camundaOrchestratorJob = new CamundaOrchestratorJob(jobDTO.getUuid(),
                externalTaskService, externalTask);
        mateoOrchestratorService.startedJobs.add(camundaOrchestratorJob);

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://localhost:8080/api/job?uuid=" + jobDTO.getUuid()),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(jobDTO, HttpStatus.OK));

        Exception exception = assertThrows(ExternalTaskBusinessError.class,
                () -> mateoOrchestratorService.validateAndCompleteJob(camundaOrchestratorJob));

        //then
        assertEquals("Script result wasn't successful", exception.getMessage()); //Exception was thrown
        assertEquals(0, mateoOrchestratorService.startedJobs.size()); // Job was removed
        verify(externalTaskService, times(0)).complete(any(), anyMap()); // Task not completed
    }

    @Test
    void testSendTestskriptToMateoOrchestrator() {
        //given
        String scriptFile = "sFile";
        Map<String, Object> inputVariables = new HashMap<>();
        List<String> outputVariables = new ArrayList<>();
        String expectedUuid = "SomeUuid";

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .postForEntity(eq("http://localhost:8080/api/job/start?scriptFile=" + scriptFile + "&outputVariables"),
                        any(),
                        eq(String.class)))
                .thenReturn(new ResponseEntity<>(expectedUuid, HttpStatus.OK));

        mateoOrchestratorService
                .sendTestskriptToMateoOrchestrator(scriptFile, inputVariables, outputVariables, externalTaskService,
                        externalTask);

        //then
        assertEquals(expectedUuid, mateoOrchestratorService.startedJobs.get(0).getUuid());
        assertEquals(externalTaskService, mateoOrchestratorService.startedJobs.get(0).getExternalTaskService());
        assertEquals(externalTask, mateoOrchestratorService.startedJobs.get(0).getExternalTask());
    }

    @Test
    void testSendTestskriptToMateoOrchestrator_BAD_REQUEST() {
        //given
        String scriptFile = "sFile";
        Map<String, Object> inputVariables = new HashMap<>();
        List<String> outputVariables = new ArrayList<>();
        String expectedUuid = "SomeUuid";

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .postForEntity(eq("http://localhost:8080/api/job/start?scriptFile=" + scriptFile + "&outputVariables"),
                        any(),
                        eq(String.class)))
                .thenReturn(new ResponseEntity<>(expectedUuid, HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoOrchestratorService
                        .sendTestskriptToMateoOrchestrator(scriptFile, inputVariables, outputVariables,
                                externalTaskService,
                                externalTask));

        //then
        assertEquals(0, mateoOrchestratorService.startedJobs.size());
        assertEquals("Couldn't send testscript to orchestrator", exception.getMessage());
    }

    @Test
    void testGetJobResultFromMateoOrchestrator() {
        //given
        UUID uuid = UUID.randomUUID();
        String requestUrl = "http://localhost:8080/api/job?uuid=" + uuid;
        OrchestratorJobDTO orchestratorJobDTO = new OrchestratorJobDTO();
        orchestratorJobDTO.setUuid(uuid.toString());

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq(requestUrl),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(orchestratorJobDTO, HttpStatus.OK));

        OrchestratorJobDTO result = mateoOrchestratorService.getJobResultFromMateoOrchestrator(uuid.toString());

        //then
        assertEquals(uuid.toString(), result.getUuid());
    }

    @Test
    void testGetJobResultFromMateoOrchestrator_BadRequest() {
        //given
        String uuid = "badRequest";
        String requestUrl = "http://localhost:8080/api/job?uuid=" + uuid;

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq(requestUrl),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoOrchestratorService.getJobResultFromMateoOrchestrator(uuid));

        //then
        assertEquals("Couldn't find Job", exception.getMessage());
    }

    @Test
    void testGetJobResultFromMateoOrchestrator_NoContent() {
        //given
        String uuid = "NoContent";
        String requestUrl = "http://localhost:8080/api/job?uuid=" + uuid;

        //when
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq(requestUrl),
                        eq(OrchestratorJobDTO.class)))
                .thenReturn(new ResponseEntity<>(null, HttpStatus.NO_CONTENT));

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoOrchestratorService.getJobResultFromMateoOrchestrator(uuid));

        //then
        assertEquals("Couldn't find Job", exception.getMessage());
    }

    @Test
    void testIsMateoOrchestratorOnline() throws MalformedURLException {
        URL backupUrl = mateoOrchestratorService.mateoApiProperties.getUrl();
        mateoOrchestratorService.mateoApiProperties.setUrl("http://isOnline.de/");
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://isOnline.de/api/job/online"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Up", HttpStatus.OK));
        boolean isOnline = mateoOrchestratorService.isMateoOrchestratorOnline();
        assertTrue(isOnline);
        mateoOrchestratorService.mateoApiProperties.setUrl(backupUrl.toString());
    }

    @Test
    void testIsMateoOrchestratorOnline_Offline() throws MalformedURLException {
        URL backupUrl = mateoOrchestratorService.mateoApiProperties.getUrl();
        mateoOrchestratorService.mateoApiProperties.setUrl("http://isOnline2.de/");
        when(mateoOrchestratorService.getRestTemplate()
                .getForEntity(eq("http://isOnline2.de/api/job/online"), eq(String.class)))
                .thenReturn(new ResponseEntity<>("Up", HttpStatus.BAD_REQUEST));

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoOrchestratorService.isMateoOrchestratorOnline());

        assertEquals("Mateoorchestrator is offline", exception.getMessage());
        mateoOrchestratorService.mateoApiProperties.setUrl(backupUrl.toString());
    }

    @Test
    void testGetPrioJob_Count() {
        CamundaOrchestratorJob firstJob = new CamundaOrchestratorJob("one", externalTaskService, externalTask);
        firstJob.setAskedTimes(3);
        CamundaOrchestratorJob secondJob = new CamundaOrchestratorJob("two", externalTaskService, externalTask);
        secondJob.setAskedTimes(1);
        CamundaOrchestratorJob thirdJob = new CamundaOrchestratorJob("three", externalTaskService, externalTask);
        thirdJob.setAskedTimes(2);

        mateoOrchestratorService.startedJobs.add(firstJob);
        mateoOrchestratorService.startedJobs.add(secondJob);
        mateoOrchestratorService.startedJobs.add(thirdJob);

        CamundaOrchestratorJob prioJob = mateoOrchestratorService.getPrioJob();

        assertEquals(secondJob, prioJob);
    }

    @Test
    void testGetPrioJob_SameTimes_DateTime() throws ParseException {
        CamundaOrchestratorJob firstJob = new CamundaOrchestratorJob("one", externalTaskService, externalTask);
        firstJob.setAskedTimes(5);
        firstJob.setStarted(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2020"));
        CamundaOrchestratorJob secondJob = new CamundaOrchestratorJob("two", externalTaskService, externalTask);
        secondJob.setAskedTimes(1);
        secondJob.setStarted(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2021"));
        CamundaOrchestratorJob thirdJob = new CamundaOrchestratorJob("three", externalTaskService, externalTask);
        thirdJob.setAskedTimes(1);
        thirdJob.setStarted(new SimpleDateFormat("dd.MM.yyyy").parse("01.01.2020"));

        mateoOrchestratorService.startedJobs.add(firstJob);
        mateoOrchestratorService.startedJobs.add(secondJob);
        mateoOrchestratorService.startedJobs.add(thirdJob);

        CamundaOrchestratorJob prioJob = mateoOrchestratorService.getPrioJob();

        assertEquals(thirdJob, prioJob);
    }

    @Test
    void testGetPrioJob_EmptyList() {
        CamundaOrchestratorJob prioJob = mateoOrchestratorService.getPrioJob();
        assertNull(prioJob);
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