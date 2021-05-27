package de.viadee.mateocamundabridge.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.ExternalTaskBusinessError;
import de.viadee.bpm.camunda.externaltask.retry.aspect.error.InstantIncidentException;
import de.viadee.mateocamundabridge.Constants;
import de.viadee.mateocamundabridge.ProcessConstants;
import de.viadee.mateocamundabridge.dtos.ReportDTO;
import de.viadee.mateocamundabridge.exceptions.MateoBridgeRuntimeException;
import de.viadee.mateocamundabridge.properties.MateoApiProperties;
import de.viadee.mateocamundabridge.utils.VariableConverter;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

@Service
public class MateoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MateoService.class);

    private static final String API_STATUS = "api/status";

    private static final String API_EXECUTE_SCRIPT_FILES = "api/execute/script-files";

    private static final String PARAM_FILENAME = "filename";

    private static final String API_STORAGE_SET = "api/storage/set";

    private static final String PARAM_KEY = "key";

    private static final String PARAM_VALUE = "value";

    private static final String API_STORAGE_GET = "api/storage/get";

    private static final String NOT_FOUND = "notFound";

    private static final String ABORTED = "Aborted";

    private static final String SSL = "SSL";

    private static final String EMPTY_STRING = "";

    private final RestTemplate restTemplate;

    protected final MateoApiProperties mateoApiProperties;

    public MateoService(RestTemplateBuilder restTemplateBuilder, MateoApiProperties mateoApiProperties)
            throws KeyManagementException, NoSuchAlgorithmException {
        this.mateoApiProperties = mateoApiProperties;
        if (mateoApiProperties.getAuthentication().isEnable()) {
            this.restTemplate = generateAuthRestTemplate(restTemplateBuilder, mateoApiProperties);
        } else {
            this.restTemplate = restTemplateBuilder.build();
        }
    }

    /**
     * set storage variables, then start script und after that, get values of storage variables
     *
     * @param scriptName   script to start
     * @param inputParams  input variables for the testscript
     * @param outputParams output variables for the testscript
     * @return response from mateo
     */
    public ReportDTO startScriptWithVariables(String scriptName, Map<String, Object> inputParams,
            List<String> outputParams) {
        setStorageVariables(VariableConverter.toMapStringString(inputParams));
        ReportDTO reportDTO = startScript(scriptName);
        reportDTO.setOutputVariables(getStorageVariables(outputParams));
        reportDTO.getOutputVariables()
                .put(ProcessConstants.EXTOUT_MATEO_SCRIPT_RESULT, reportDTO.getResultString());

        if (!reportDTO.getResultString().equals(Constants.ERFOLG) && !reportDTO.getResultString()
                .equals(Constants.SUCCESSFUL))
            throw new ExternalTaskBusinessError(mateoApiProperties.getErrorCode(), "Script run wasn't successful",
                    VariableConverter.toEngineValues(reportDTO.getOutputVariables()));

        return reportDTO;
    }

    /**
     * get Value of storage variables (uses mateo rest-api)
     *
     * @param resultVariables List of keys to get the values from mateo
     * @return HashMap of storage variables mit keys from inputParameter and values from mateo (storage)
     */
    public Map<String, Object> getStorageVariables(List<String> resultVariables) {
        Map<String, Object> resultMap = new HashMap<>();
        boolean failed = false;
        ResponseEntity<String> responseEntity;
        LOGGER.info("Read storage variables...");
        for (String variable : resultVariables) {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme(mateoApiProperties.getUrl().getProtocol())
                    .host(mateoApiProperties.getUrl().getHost())
                    .port(mateoApiProperties.getUrl().getPort())
                    .path(API_STORAGE_GET)
                    .queryParam(PARAM_KEY, variable)
                    .build()
                    .toUri();
            responseEntity = restTemplate.getForEntity(uri.toString(), String.class);
            if (responseEntity.getStatusCodeValue() >= 300) {
                LOGGER.warn("Variable: {} could not be read", variable);
                failed = true;
                resultMap.put(variable, NOT_FOUND);
            } else if (responseEntity.getStatusCode().equals(HttpStatus.NO_CONTENT) ||
                    Objects.isNull(responseEntity.getBody())) {
                resultMap.put(variable, EMPTY_STRING);
            } else {
                resultMap.put(variable, responseEntity.getBody());
            }
        }
        if (failed)
            throw new ExternalTaskBusinessError(mateoApiProperties.getErrorCode(),
                    "Some variables couldn't be read from mateo", resultMap);
        LOGGER.info("Storage variables read out: {}", resultMap);
        return resultMap;
    }

    /**
     * Write variables (key, value) to the mateo storage (uses mateo rest-api)
     *
     * @param scriptVariables Map of variables to write to storage
     */
    public void setStorageVariables(Map<String, String> scriptVariables) {
        LOGGER.info("Write variables to storage: {}", scriptVariables);
        for (Map.Entry<String, String> entry : scriptVariables.entrySet()) {
            URI uri = UriComponentsBuilder.newInstance()
                    .scheme(mateoApiProperties.getUrl().getProtocol())
                    .host(mateoApiProperties.getUrl().getHost())
                    .port(mateoApiProperties.getUrl().getPort())
                    .path(API_STORAGE_SET)
                    .queryParam(PARAM_KEY, entry.getKey())
                    .queryParam(PARAM_VALUE, entry.getValue())
                    .build()
                    .toUri();
            HttpHeaders headers = getHttpHeaders();
            Map<String, Object> map = new HashMap<>();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
            ResponseEntity<String> response = this.restTemplate.postForEntity(uri.toString(), entity, String.class);

            if (response.getStatusCodeValue() >= 300) {
                throw new MateoBridgeRuntimeException(
                        String.format("Variable '%s' with value '%s' could not be written to the storage file.",
                                entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * start mateo script via mateo rest-api
     *
     * @param scriptName script path of the script to run (script has to be in the environment script directory of mateo)
     * @return response from mateo
     */
    public ReportDTO startScript(String scriptName) {
        LOGGER.info("Run the script: {}", scriptName);
        URI uri = UriComponentsBuilder.newInstance()
                .scheme(mateoApiProperties.getUrl().getProtocol())
                .host(mateoApiProperties.getUrl().getHost())
                .port(mateoApiProperties.getUrl().getPort())
                .path(API_EXECUTE_SCRIPT_FILES)
                .queryParam(PARAM_FILENAME, scriptName)
                .build()
                .toUri();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = this.restTemplate
                .postForEntity(uri.toString(), entity, String.class);

        if (response.hasBody() && Objects.equals(response.getBody(), ABORTED))
            throw new MateoBridgeRuntimeException("Script execution aborted");

        if (response.getStatusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR))
            throw new MateoBridgeRuntimeException(response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        ReportDTO resultDto;
        try {
            resultDto = objectMapper.readValue(response.getBody(), ReportDTO.class);
        } catch (JsonProcessingException e) {
            throw new InstantIncidentException("Couldn't parse answer from mateo", e);
        }
        return resultDto;
    }

    /**
     * check if mateo is reachable/online
     *
     * @return true if mateo is online
     */
    public boolean isMateoOnline() {
        URI uri = UriComponentsBuilder.newInstance()
                .scheme(mateoApiProperties.getUrl().getProtocol())
                .host(mateoApiProperties.getUrl().getHost())
                .port(mateoApiProperties.getUrl().getPort())
                .path(API_STATUS)
                .build()
                .toUri();

        ResponseEntity<String> response = restTemplate.getForEntity(uri.toString(), String.class);

        if (response.getStatusCodeValue() >= 300)
            throw new MateoBridgeRuntimeException("Mateo is offline");

        return true;
    }

    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    private RestTemplate generateAuthRestTemplate(RestTemplateBuilder builder, MateoApiProperties mateoApiProperties)
            throws NoSuchAlgorithmException,
            KeyManagementException {

        MateoApiProperties.Authentication authentication = mateoApiProperties.getAuthentication();

        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {

                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        SSLContext sslContext = SSLContext.getInstance(SSL);
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
        HttpComponentsClientHttpRequestFactory customRequestFactory = new HttpComponentsClientHttpRequestFactory();
        customRequestFactory.setHttpClient(httpClient);

        return builder.basicAuthentication(authentication.getUsername(), authentication.getPassword())
                .requestFactory(() -> customRequestFactory).build();
    }
}
