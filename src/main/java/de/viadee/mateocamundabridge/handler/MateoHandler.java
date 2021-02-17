package de.viadee.mateocamundabridge.handler;

import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.Constants;
import de.viadee.mateocamundabridge.ProcessConstants;
import de.viadee.mateocamundabridge.dtos.ReportDTO;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import de.viadee.mateocamundabridge.services.MateoOrchestratorService;
import de.viadee.mateocamundabridge.services.MateoService;
import de.viadee.mateocamundabridge.utils.VariableConverter;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MateoHandler implements ExternalTaskHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MateoHandler.class);

    private final MateoService mateoService;

    private final MateoOrchestratorService mateoOrchestratorService;

    private final MateoApiProperties mateoApiProperties;

    public MateoHandler(MateoService mateoService, MateoOrchestratorService mateoOrchestratorService,
            MateoApiProperties mateoApiProperties) {
        this.mateoService = mateoService;
        this.mateoOrchestratorService = mateoOrchestratorService;
        this.mateoApiProperties = mateoApiProperties;
    }

    /**
     * if task with topic is fetched, run this method
     * get camunda extension property (script file) and variables from input mapping
     * send camunda variables to mateo and wait for result. then complete the external task
     *
     * @param externalTask        fetched external Task
     * @param externalTaskService external Task Service
     */
    public void execute(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
        LOGGER.debug("Locked External Task with Id {}", externalTask.getId());

        String scriptPath = externalTask.getExtensionProperty(ProcessConstants.EXTIN_MATEOSCRIPT);
        if (Objects.isNull(scriptPath))
            throw new InstantIncidentException(
                    "Missing script path. Please add 'MATEO_SCRIPT' under 'Extensions - Properties'");

        Map<String, Object> inputParams = externalTask.getVariable(ProcessConstants.EXTIN_MATEO_PARAMS);
        if (Objects.isNull(inputParams))
            throw new InstantIncidentException(
                    "Missing input parameter. Please add Map 'extIn_mateoParams' under 'Input Parameters'");

        List<String> outputParams = externalTask.getVariable(ProcessConstants.EXTOUT_MATEO_PARAMS);
        if (Objects.isNull(outputParams))
            throw new InstantIncidentException(
                    "Missing output parameter. Please add List 'extOut_mateoParams' under 'Input Parameters'");

        if (mateoApiProperties.getType().equals(Constants.ORCHESTRATOR) && mateoOrchestratorService
                .isMateoOrchestratorOnline()) {
            mateoOrchestratorService.sendTestskriptToMateoOrchestrator(scriptPath, inputParams, outputParams,
                    externalTaskService, externalTask);
        } else if (mateoApiProperties.getType().equals(Constants.MATEO) && mateoService.isMateoOnline()) {
            ReportDTO reportDTO = mateoService.startScriptWithVariables(scriptPath, inputParams, outputParams);

            LOGGER.info("Complete External Task: {}", externalTask.getId());
            externalTaskService.complete(externalTask,
                    VariableConverter.toEngineValues(reportDTO.getOutputVariables()));
        }
    }
}
