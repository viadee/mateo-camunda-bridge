package de.viadee.mateocamundabridge.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Marcel_Flasskamp
 */
@Validated
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "de.viadee.mateo.rpa.mateo-api")
public class MateoApiProperties {

    private String topic = "mateo";

    private String type;

    private URL url;

    private String errorCode = "mateoError";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    private Authentication authentication = new Authentication();

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public URL getUrl() {
        return url;
    }

    public void setUrl(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public static class Authentication {

        private boolean enable = false;

        private String username = "";

        private String password = "";

        public boolean isEnable() {
            return enable;
        }

        public void setEnable(boolean enable) {
            this.enable = enable;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
