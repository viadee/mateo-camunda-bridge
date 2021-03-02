package de.viadee.mateocamundabridge.dtos;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OrchestratorJobDTO {

    private String uuid;

    private String scriptName;

    private Map<String, String> inputVariables = new HashMap<>();

    private Map<String, String> outputVariables = new HashMap<>();

    private String status;

    private String priority;

    private Integer priorityValue;

    private Integer askedTimes;

    private Date createDate;

    private Date modifyDate;

    private String filePath;

    private String filename;

    private String testSetName;

    private String resultLevel;

    private String resultString;

    private String startTime;

    private String originFileName;

    private String runIndex;

    private String vtfVersion;

    private String mateoInstanz;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public Map<String, String> getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(Map<String, String> outputVariables) {
        this.outputVariables = outputVariables;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getPriorityValue() {
        return priorityValue;
    }

    public void setPriorityValue(Integer priorityValue) {
        this.priorityValue = priorityValue;
    }

    public Integer getAskedTimes() {
        return askedTimes;
    }

    public void setAskedTimes(Integer askedTimes) {
        this.askedTimes = askedTimes;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getTestSetName() {
        return testSetName;
    }

    public void setTestSetName(String testSetName) {
        this.testSetName = testSetName;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public String getResultString() {
        return resultString;
    }

    public void setResultString(String resultString) {
        this.resultString = resultString;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getOriginFileName() {
        return originFileName;
    }

    public void setOriginFileName(String originFileName) {
        this.originFileName = originFileName;
    }

    public String getRunIndex() {
        return runIndex;
    }

    public void setRunIndex(String runIndex) {
        this.runIndex = runIndex;
    }

    public String getVtfVersion() {
        return vtfVersion;
    }

    public void setVtfVersion(String vtfVersion) {
        this.vtfVersion = vtfVersion;
    }

    public String getMateoInstanz() {
        return mateoInstanz;
    }

    public void setMateoInstanz(String mateoInstanz) {
        this.mateoInstanz = mateoInstanz;
    }

    @Override public String toString() {
        return "OrchestratorJobDTO{" +
                "uuid='" + uuid + '\'' +
                ", scriptName='" + scriptName + '\'' +
                ", inputVariables=" + inputVariables +
                ", outputVariables=" + outputVariables +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", priorityValue=" + priorityValue +
                ", askedTimes=" + askedTimes +
                ", createDate=" + createDate +
                ", modifyDate=" + modifyDate +
                ", filePath='" + filePath + '\'' +
                ", filename='" + filename + '\'' +
                ", testSetName='" + testSetName + '\'' +
                ", resultLevel='" + resultLevel + '\'' +
                ", resultString='" + resultString + '\'' +
                ", startTime='" + startTime + '\'' +
                ", originFileName='" + originFileName + '\'' +
                ", runIndex='" + runIndex + '\'' +
                ", vtfVersion='" + vtfVersion + '\'' +
                ", mateoInstanz='" + mateoInstanz + '\'' +
                '}';
    }
}
