package de.viadee.mateocamundabridge.handler;

import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.Constants;
import de.viadee.mateocamundabridge.ProcessConstants;
import de.viadee.mateocamundabridge.dtos.ReportDTO;
import de.viadee.mateocamundabridge.exceptions.MateoBridgeRuntimeException;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import de.viadee.mateocamundabridge.services.MateoOrchestratorService;
import de.viadee.mateocamundabridge.services.MateoService;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * @author Marcel_Flasskamp
 */
@ExtendWith(SpringExtension.class)
@SpringJUnitConfig
@SpringBootTest
class MateoHandlerTest {

    private final MateoService mateoService = mock(MateoService.class, Mockito.RETURNS_DEEP_STUBS);

    private final MateoOrchestratorService mateoOrchestratorService = mock(MateoOrchestratorService.class,
            Mockito.RETURNS_DEEP_STUBS);

    private final MateoApiProperties mateoApiProperties = mock(MateoApiProperties.class, Mockito.RETURNS_DEEP_STUBS);

    private final MateoHandler mateoHandler = new MateoHandler(mateoService, mateoOrchestratorService,
            mateoApiProperties);

    private final ExternalTask externalTask = mock(ExternalTask.class, Mockito.RETURNS_DEEP_STUBS);

    private final ExternalTaskService externalTaskService = mock(ExternalTaskService.class, Mockito.RETURNS_DEEP_STUBS);

    @Test
    void testExecute_NoScriptPath() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn(null);

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoHandler.execute(externalTask, externalTaskService));

        assertEquals("Missing script path. Please add 'MATEO_SCRIPT' under 'Extensions - Properties'",
                exception.getMessage());
    }

    @Test
    void testExecute_NoInputParams() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(null);

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoHandler.execute(externalTask, externalTaskService));

        assertEquals("Missing input parameter. Please add Map 'extIn_mateoParams' under 'Input Parameters'",
                exception.getMessage());
    }

    @Test
    void testExecute_NoOutputParams() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(new HashMap<>());
        when(externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS)).thenReturn(null);

        Exception exception = assertThrows(InstantIncidentException.class,
                () -> mateoHandler.execute(externalTask, externalTaskService));

        assertEquals("Missing output parameter. Please add List 'extOut_mateoParams' under 'Input Parameters'",
                exception.getMessage());
    }

    @Test
    void testExecute_OrchestratorOffline() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(new HashMap<>());
        when(externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS)).thenReturn(new ArrayList<String>());
        when(mateoOrchestratorService.isMateoOrchestratorOnline())
                .thenThrow(new MateoBridgeRuntimeException("Mateoorchestrator is offline"));
        when(mateoApiProperties.getType()).thenReturn(Constants.ORCHESTRATOR);

        Exception exception = assertThrows(MateoBridgeRuntimeException.class,
                () -> mateoHandler.execute(externalTask, externalTaskService));

        assertEquals("Mateoorchestrator is offline", exception.getMessage());
    }

    @Test
    void testExecute_MateoOffline() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(new HashMap<>());
        when(externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS)).thenReturn(new ArrayList<String>());
        when(mateoService.isMateoOnline()).thenThrow(new RestClientException("Mateo is offline"));
        when(mateoApiProperties.getType()).thenReturn(Constants.MATEO);

        Exception exception = assertThrows(RestClientException.class,
                () -> mateoHandler.execute(externalTask, externalTaskService));

        assertEquals("Mateo is offline", exception.getMessage());
    }

    @Test
    void testExecute_Orchestrator() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(new HashMap<>());
        when(externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS)).thenReturn(new ArrayList<String>());
        when(mateoOrchestratorService.isMateoOrchestratorOnline()).thenReturn(true);
        when(mateoApiProperties.getType()).thenReturn(Constants.ORCHESTRATOR);

        mateoHandler.execute(externalTask, externalTaskService);

        verify(mateoOrchestratorService, times(1))
                .sendTestskriptToMateoOrchestrator(eq("someScript"), anyMap(), anyList(), eq(externalTaskService),
                        eq(externalTask));
    }

    @Test
    void testExecute_Mateo() {
        when(externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT)).thenReturn("someScript");
        when(externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS)).thenReturn(new HashMap<>());
        when(externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS)).thenReturn(new ArrayList<String>());
        when(mateoService.isMateoOnline()).thenReturn(true);
        when(mateoApiProperties.getType()).thenReturn(Constants.MATEO);
        when(mateoService.startScriptWithVariables(eq("someScript"), anyMap(), anyList())).thenReturn(new ReportDTO());

        mateoHandler.execute(externalTask, externalTaskService);

        verify(externalTaskService, times(1)).complete(eq(externalTask), any());
    }
}