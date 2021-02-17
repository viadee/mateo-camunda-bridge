package de.viadee.mateocamundabridge.services;

import de.viadee.bpm.camunda.externaltask.retry.aspect.error.ExternalTaskBusinessError;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.Constants;
import de.viadee.mateocamundabridge.ProcessConstants;
import de.viadee.mateocamundabridge.dtos.CamundaOrchestratorJob;
import de.viadee.mateocamundabridge.dtos.OrchestratorJobDTO;
import de.viadee.mateocamundabridge.dtos.ReportDTO;
import de.viadee.mateocamundabridge.enums.JobStatus;
import de.viadee.mateocamundabridge.exceptions.MateoBridgeRuntimeException;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import de.viadee.mateocamundabridge.utils.VariableConverter;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Marcel_Flasskamp
 * Service class to call the mateo-orchestrator
 */
@Service
public class MateoOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MateoOrchestratorService.class);

    private static final String API_JOB_START = "/api/job/start";

    private static final String API_JOB_ONLINE = "/api/job/online";

    private static final String API_JOB = "/api/job";

    private static final String PARAM_UUID = "uuid";

    private static final String PARAM_SCRIPT_FILE = "scriptFile";

    private static final String PARAM_OUTPUT_VARS = "outputVariables";

    private final RestTemplate restTemplate;

    protected final MateoApiProperties mateoApiProperties;

    protected final List<CamundaOrchestratorJob> startedJobs = new ArrayList<>();

    public MateoOrchestratorService(RestTemplateBuilder restTemplateBuilder,
            MateoApiProperties mateoApiProperties) {
        this.restTemplate = restTemplateBuilder.build();
        this.mateoApiProperties = mateoApiProperties;
    }

    /**
     * take job from startedJob List and ask mateo-orchestrator for result
     * if job is finished, delete job from list and complete task
     * scheduled every five second
     */
    @Scheduled(fixedDelay = 5000)
    public void completeTaskScheduled() {
        CamundaOrchestratorJob queuedJob = getPrioJob();
        if (!Objects.isNull(queuedJob)) {
            validateAndCompleteJob(queuedJob);
        }
    }

    public void validateAndCompleteJob(final CamundaOrchestratorJob queuedJob) {
        OrchestratorJobDTO orchestratorJob = getJobResultFromMateoOrchestrator(queuedJob.getUuid());
        if (Objects.nonNull(orchestratorJob) && validateJob(orchestratorJob, queuedJob)) {
            ExternalTaskService externalTaskService = queuedJob.getExternalTaskService();
            ExternalTask externalTask = queuedJob.getExternalTask();
            orchestratorJob.getReportDTO().getOutputVariables()
                    .put(ProcessConstants.EXTOUT_MATEO_SCRIPT_RESULT,
                            orchestratorJob.getReportDTO().getResultString());
            validateReportDto(orchestratorJob.getReportDTO(), queuedJob);
            LOGGER.info("Complete External Task with Id {}", externalTask.getId());
            externalTaskService.complete(externalTask,
                    VariableConverter.toEngineValues(orchestratorJob.getReportDTO().getOutputVariables()));
            LOGGER.info("Script was running on mateo {}",
                    orchestratorJob.getReportDTO().getMateoInstanz());
            startedJobs.remove(queuedJob);
        }
    }

    private boolean validateJob(OrchestratorJobDTO orchestratorJob, CamundaOrchestratorJob queuedJob) {
        if (orchestratorJob.getStatus() == JobStatus.FINISHED) {
            return true;
        } else if (orchestratorJob.getStatus() == JobStatus.FAILED) {
            startedJobs.remove(queuedJob);
            throw new ExternalTaskBusinessError(mateoApiProperties.getErrorCode(), "Job failed",
                    VariableConverter.toEngineValues(orchestratorJob.getReportDTO().getOutputVariables()));
        } else {
            startedJobs.get(startedJobs.indexOf(queuedJob)).count();
            return false;
        }
    }

    private void validateReportDto(final ReportDTO reportDTO, final CamundaOrchestratorJob queuedJob) {
        if (!reportDTO.getResultString().equals(Constants.ERFOLG) && !reportDTO.getResultString()
                .equals(Constants.SUCCESSFUL)) {
            startedJobs.remove(queuedJob);
            throw new ExternalTaskBusinessError(mateoApiProperties.getErrorCode(), "Script result wasn't successful",
                    VariableConverter.toEngineValues(reportDTO.getOutputVariables()));
        }
        if (reportDTO.getOutputVariables().containsValue(Constants.NOT_FOUND)) {
            startedJobs.remove(queuedJob);
            throw new ExternalTaskBusinessError(mateoApiProperties.getErrorCode(), "Error while reading variables",
                    VariableConverter.toEngineValues(reportDTO.getOutputVariables()));
        }
    }

    /**
     * sort started jobs and return first in list
     * sort by: asked Times and job started time
     * @return Prioritized job
     */
    protected CamundaOrchestratorJob getPrioJob() {
        List<CamundaOrchestratorJob> sortedList = startedJobs.stream()
                .sorted(Comparator.comparingInt(CamundaOrchestratorJob::getAskedTimes))
                .sorted(Comparator.comparing(CamundaOrchestratorJob::getStarted))
                .collect(Collectors.toList());

        return sortedList.isEmpty() ? null : sortedList.get(0);
    }

    /**
     * send script file path incl. variables to the mateo-orchestrator and put the job to the started job list
     *
     * @param scriptFile      filepath to send to the mateo-orchestrator
     * @param inputVariables  in/out variables for the testskript
     * @param externalService camunda externalTaskService
     * @param externalTask    camunda ExternalTask
     */
    public void sendTestskriptToMateoOrchestrator(String scriptFile, Map<String, Object> inputVariables,
            List<String> outputVariables,
            ExternalTaskService externalService, ExternalTask externalTask) {

        URI uri = UriComponentsBuilder.newInstance()
                .scheme(mateoApiProperties.getUrl().getProtocol())
                .host(mateoApiProperties.getUrl().getHost())
                .port(mateoApiProperties.getUrl().getPort())
                .path(API_JOB_START)
                .queryParam(PARAM_SCRIPT_FILE, scriptFile)
                .queryParam(PARAM_OUTPUT_VARS, outputVariables)
                .build()
                .toUri();

        HttpHeaders headers = getHttpHeaders();
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(
                inputVariables != null ? VariableConverter.toMapStringString(inputVariables) : new HashMap<>(),
                headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uri.toString(), entity, String.class);
        if (response.getStatusCodeValue() < 300) {
            CamundaOrchestratorJob startedJob = new CamundaOrchestratorJob(response.getBody(), externalService,
                    externalTask);
            startedJobs.add(startedJob);
        } else {
            throw new MateoBridgeRuntimeException("Couldn't send testscript to orchestrator");
        }
    }

    /**
     * ask the mateo-orchestrator for job status, if finished, then return jobDto
     * try at least 20 attempts with delay of 5 seconds
     *
     * @param uuid the uuid of the started job to get response
     * @return jobDto of finished job
     */
    public OrchestratorJobDTO getJobResultFromMateoOrchestrator(String uuid) {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme(mateoApiProperties.getUrl().getProtocol())
                .host(mateoApiProperties.getUrl().getHost())
                .port(mateoApiProperties.getUrl().getPort())
                .path(API_JOB)
                .queryParam(PARAM_UUID, uuid)
                .build()
                .toUri();

        ResponseEntity<OrchestratorJobDTO> response = restTemplate
                .getForEntity(uri.toString(), OrchestratorJobDTO.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        } else {
            startedJobs.removeIf(job -> job.getUuid().equals(uuid));
            throw new InstantIncidentException("Couldn't find Job");
        }
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * @return true whether mateo-orchestrator is online/reachable
     */
    public boolean isMateoOrchestratorOnline() {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme(mateoApiProperties.getUrl().getProtocol())
                .host(mateoApiProperties.getUrl().getHost())
                .port(mateoApiProperties.getUrl().getPort())
                .path(API_JOB_ONLINE)
                .build()
                .toUri();
        ResponseEntity<String> response = restTemplate.getForEntity(uri.toString(), String.class);

        if (response.getStatusCodeValue() >= 300)
            throw new MateoBridgeRuntimeException("Mateoorchestrator is offline");

        return true;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }
}
