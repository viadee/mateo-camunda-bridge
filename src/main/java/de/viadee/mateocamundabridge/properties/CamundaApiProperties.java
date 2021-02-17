package de.viadee.mateocamundabridge.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.net.MalformedURLException;
import java.net.URL;

@Validated
@Component
@ConfigurationProperties(prefix = "de.viadee.mateo.rpa.camunda-api")
public class CamundaApiProperties {

    private String url;

    private long lockDuration = 3600000;

    private long lockFactor = 2;

    private long lockMaxTime = 600;

    private long initTime = 500;

    private Authentication authentication;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) throws MalformedURLException {
        if (!url.contains("engine-rest"))
            throw new MalformedURLException("Camunda rest endpoint missing or incorrect");
        this.url = new URL(url).toString();
    }

    public long getLockDuration() {
        return lockDuration;
    }

    public void setLockDuration(long lockDuration) {
        this.lockDuration = lockDuration;
    }

    public long getLockFactor() {
        return lockFactor;
    }

    public void setLockFactor(long lockFactor) {
        this.lockFactor = lockFactor;
    }

    public long getLockMaxTime() {
        return lockMaxTime;
    }

    public void setLockMaxTime(long lockMaxTime) {
        this.lockMaxTime = lockMaxTime;
    }

    public long getInitTime() {
        return initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    @Override public String toString() {
        return "CamundaApiProperties{" +
                "url='" + url + '\'' +
                ", lockDuration=" + lockDuration +
                ", lockFactor=" + lockFactor +
                ", lockMaxTime=" + lockMaxTime +
                ", initTime=" + initTime +
                ", authentication=" + authentication.toString() +
                '}';
    }

    public static class Authentication {

        private String type;

        private String username;

        private String password;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        @Override public String toString() {
            return "Authentication{" +
                    "type='" + type + '\'' +
                    ", username='" + username + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }

    }
}