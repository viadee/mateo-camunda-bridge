package de.viadee.mateocamundabridge.dtos;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Marcel_Flasskamp
 */
public class ReportDTO {

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

    private Map<String, Object> outputVariables = new HashMap<>();

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setTestSetName(String testSetName) {
        this.testSetName = testSetName;
    }

    public void setResultLevel(String resultLevel) {
        this.resultLevel = resultLevel;
    }

    public void setResultString(String resultString) {
        this.resultString = resultString;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setOriginFileName(String originFileName) {
        this.originFileName = originFileName;
    }

    public void setRunIndex(String runIndex) {
        this.runIndex = runIndex;
    }

    public void setVtfVersion(String vtfVersion) {
        this.vtfVersion = vtfVersion;
    }

    public void setMateoInstanz(String mateoInstanz) {
        this.mateoInstanz = mateoInstanz;
    }

    public void setOutputVariables(Map<String, Object> outputVariables) {
        this.outputVariables = outputVariables;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFilename() {
        return filename;
    }

    public String getTestSetName() {
        return testSetName;
    }

    public String getResultLevel() {
        return resultLevel;
    }

    public String getResultString() {
        return resultString;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getOriginFileName() {
        return originFileName;
    }

    public String getRunIndex() {
        return runIndex;
    }

    public String getVtfVersion() {
        return vtfVersion;
    }

    public String getMateoInstanz() {
        return mateoInstanz;
    }

    public Map<String, Object> getOutputVariables() {
        return outputVariables;
    }

    @Override public String toString() {
        return "ReportDTO{" +
                "filePath='" + filePath + '\'' +
                ", filename='" + filename + '\'' +
                ", testSetName='" + testSetName + '\'' +
                ", resultLevel='" + resultLevel + '\'' +
                ", resultString='" + resultString + '\'' +
                ", startTime='" + startTime + '\'' +
                ", originFileName='" + originFileName + '\'' +
                ", runIndex='" + runIndex + '\'' +
                ", vtfVersion='" + vtfVersion + '\'' +
                ", mateoInstanz='" + mateoInstanz + '\'' +
                ", outputVariables=" + outputVariables.toString() +
                '}';
    }
}
