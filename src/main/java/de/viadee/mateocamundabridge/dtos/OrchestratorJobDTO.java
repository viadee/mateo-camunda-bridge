package de.viadee.mateocamundabridge.dtos;

import de.viadee.mateocamundabridge.enums.JobStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcel_Flasskamp
 */
public class OrchestratorJobDTO {

    private String uuid;

    private String scriptName;

    private Map<String, String> inputVariables = new HashMap<>();

    private JobStatus status;

    private ReportDTO reportDTO = new ReportDTO();

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getScriptName() {
        return scriptName;
    }

    public void setScriptName(String scriptName) {
        this.scriptName = scriptName;
    }

    public Map<String, String> getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(Map<String, String> inputVariables) {
        this.inputVariables = inputVariables;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public ReportDTO getReportDTO() {
        return reportDTO;
    }

    public void setReportDTO(ReportDTO reportDTO) {
        this.reportDTO = reportDTO;
    }

    @Override
    public String toString() {
        return "JobDTO{" +
                "uuid='" + uuid + '\'' +
                ", scriptName='" + scriptName + '\'' +
                ", inputVariables=" + inputVariables.toString() +
                ", status=" + status.toString() +
                ", reportDTO=" + reportDTO.toString() +
                '}';
    }
}
