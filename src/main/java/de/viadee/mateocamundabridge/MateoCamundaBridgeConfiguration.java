package de.viadee.mateocamundabridge;

import de.viadee.mateocamundabridge.handler.MateoHandler;
import de.viadee.mateocamundabridge.properties.CamundaApiProperties;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.ExternalTaskClientBuilder;
import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.backoff.ExponentialBackoffStrategy;
import org.camunda.bpm.client.impl.ExternalTaskClientBuilderImpl;
import org.camunda.bpm.client.interceptor.auth.BasicAuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.UUID;

/**
 * @author Marcel_Flasskamp
 */
@Configuration
public class MateoCamundaBridgeConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(MateoCamundaBridgeConfiguration.class);

    private final CamundaApiProperties camundaApiProperties;

    private final MateoHandler mateoHandler;

    private final MateoApiProperties mateoApiProperties;

    private static final String WORKER_ID = "Mateo-Camunda-RPA-Bridge-" + UUID.randomUUID();

    public MateoCamundaBridgeConfiguration(
            CamundaApiProperties camundaApiProperties,
            MateoHandler mateoHandler, MateoApiProperties mateoApiProperties) {
        this.camundaApiProperties = camundaApiProperties;
        this.mateoHandler = mateoHandler;
        this.mateoApiProperties = mateoApiProperties;
    }

    private ExternalTaskClientBuilder externalTaskClientBuilder() {
        return new ExternalTaskClientBuilderImpl();
    }

    /**
     * initTime in milliseconds for which the client is suspended after the first request
     * lockFactor is the base of the power by which the waiting time increases
     * LockMaxTime in milliseconds for which the client can be suspended
     */
    private BackoffStrategy backoffStrategy() {
        return new ExponentialBackoffStrategy(camundaApiProperties.getInitTime(), camundaApiProperties.getLockFactor(),
                camundaApiProperties.getLockMaxTime());
    }

    private ExternalTaskClient externalTaskClient() {
        ExternalTaskClientBuilder externalTaskClientBuilder = this.externalTaskClientBuilder()
                .baseUrl(camundaApiProperties.getUrl())
                .backoffStrategy(this.backoffStrategy())
                .maxTasks(1)
                .lockDuration(camundaApiProperties.getLockDuration())
                .workerId(WORKER_ID);

        handleAuthentication(externalTaskClientBuilder);

        return externalTaskClientBuilder.build();
    }

    private void handleAuthentication(ExternalTaskClientBuilder client) {
        CamundaApiProperties.Authentication authentication = camundaApiProperties.getAuthentication();
        if (authentication != null && "basic".equals(authentication.getType())) {
            if (!StringUtils.hasText(authentication.getUsername()) || !StringUtils
                    .hasText(authentication.getPassword())) {
                throw new IllegalStateException(
                        "Application properties under 'de.viadee.mateo.rpa.camunda-api.authentication' need to contain a defined 'username' and 'password' when a 'type' is defined.");
            }
            client.addInterceptor(new BasicAuthProvider(authentication.getUsername(), authentication.getPassword()));
        }
    }

    @PostConstruct
    public void registerHandler() {
        LOGGER.info("register task-handler to topic '{}'", mateoApiProperties.getTopic());

        if (mateoApiProperties.getType().equalsIgnoreCase(Constants.ORCHESTRATOR) || mateoApiProperties.getType()
                .equalsIgnoreCase(Constants.MATEO)) {
            this.externalTaskClient()
                    .subscribe(mateoApiProperties.getTopic())
                    .includeExtensionProperties(true)
                    .localVariables(true)
                    .handler(mateoHandler)
                    .open();
        } else {
            throw new IllegalArgumentException(
                    "Couldn't start. Please add type to 'de.viadee.mateo.rpa.mateo-api.type' ('mateo' or 'orchestrator')");
        }

    }
}
