package de.viadee.mateocamundabridge.aspect;

import de.viadee.bpm.camunda.externaltask.retry.aspect.error.ExternalTaskBusinessError;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.bpm.camunda.externaltask.retry.aspect.service.BusinessErrorService;
import de.viadee.bpm.camunda.externaltask.retry.aspect.service.FailureService;
import de.viadee.mateocamundabridge.dtos.CamundaOrchestratorJob;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class ExternalTaskRetryAspectBridge {

    private final BusinessErrorService businessErrorService;
    private final FailureService failureService;

    public ExternalTaskRetryAspectBridge(final BusinessErrorService businessErrorService, final FailureService failureService) {
        this.businessErrorService = businessErrorService;
        this.failureService = failureService;
    }

    @Pointcut(value = "execution(public void  de.viadee.mateocamundabridge.services.MateoOrchestratorService.validateAndCompleteJob(..)) " +
            "&& args(queuedJob)",
            argNames = "queuedJob")
    public void externalTaskHandlerExecute(final CamundaOrchestratorJob queuedJob) {
    }

    @AfterThrowing(pointcut = "externalTaskHandlerExecute(queuedJob)",
            throwing = "exception", argNames = "joinPoint,exception,queuedJob")
    public void handleErrorAfterThrown(final JoinPoint joinPoint,
            final Exception exception,
            final CamundaOrchestratorJob queuedJob) {

        if (exception instanceof ExternalTaskBusinessError) {
            this.businessErrorService.handleError(joinPoint.getTarget().getClass(), queuedJob.getExternalTask(), queuedJob.getExternalTaskService(), (ExternalTaskBusinessError) exception);

        } else if (exception instanceof InstantIncidentException) {
            this.failureService.handleFailure(joinPoint.getTarget().getClass(), queuedJob.getExternalTask(), queuedJob.getExternalTaskService(), exception, true);

        } else {
            this.failureService.handleFailure(joinPoint.getTarget().getClass(), queuedJob.getExternalTask(), queuedJob.getExternalTaskService(), exception);

        }
    }
}
