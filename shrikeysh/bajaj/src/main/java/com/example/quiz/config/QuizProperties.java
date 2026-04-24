package com.example.quiz.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class QuizProperties {

    private Mode mode = Mode.FIXTURE;
    private String regNo = "2024CS101";
    private String baseUrl = "";
    private int pollCount = 10;
    private long delayMillis = 5000L;
    private String fixturePath = "classpath:data/sample-polls.json";
    private boolean liveSubmitEnabled = false;

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getPollCount() {
        return pollCount;
    }

    public void setPollCount(int pollCount) {
        this.pollCount = pollCount;
    }

    public long getDelayMillis() {
        return delayMillis;
    }

    public void setDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    public String getFixturePath() {
        return fixturePath;
    }

    public void setFixturePath(String fixturePath) {
        this.fixturePath = fixturePath;
    }

    public boolean isLiveSubmitEnabled() {
        return liveSubmitEnabled;
    }

    public void setLiveSubmitEnabled(boolean liveSubmitEnabled) {
        this.liveSubmitEnabled = liveSubmitEnabled;
    }

    public boolean isLiveMode() {
        return mode == Mode.LIVE;
    }

    public enum Mode {
        FIXTURE,
        LIVE
    }
}

