package de.viadee.mateocamundabridge.dtos;

import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;

import java.util.Date;

public class CamundaOrchestratorJob {

    private String uuid;

    private ExternalTask externalTask;

    private ExternalTaskService externalTaskService;

    private Date started;

    private int askedTimes;

    public CamundaOrchestratorJob(String uuid, ExternalTaskService externalTaskService, ExternalTask externalTask) {
        this.uuid = uuid;
        this.externalTask = externalTask;
        this.externalTaskService = externalTaskService;
        this.started = new Date();
    }

    public void count() {
        askedTimes++;
    }

    public ExternalTask getExternalTask() {
        return externalTask;
    }

    public void setExternalTask(ExternalTask externalTask) {
        this.externalTask = externalTask;
    }

    public ExternalTaskService getExternalTaskService() {
        return externalTaskService;
    }

    public void setExternalTaskService(ExternalTaskService externalTaskService) {
        this.externalTaskService = externalTaskService;
    }

    public Date getStarted() {
        return started;
    }

    public void setStarted(Date started) {
        this.started = started;
    }

    public int getAskedTimes() {
        return askedTimes;
    }

    public void setAskedTimes(int askedTimes) {
        this.askedTimes = askedTimes;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }
}
