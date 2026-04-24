package com.example.quiz.client;

import com.example.quiz.config.QuizProperties;
import com.example.quiz.model.ApiResponse;
import com.example.quiz.model.LeaderboardEntry;
import com.example.quiz.model.SubmissionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApiClient {

    private static final Logger log = LoggerFactory.getLogger(ApiClient.class);

    private final QuizProperties properties;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private volatile Map<Integer, ApiResponse> fixtureCache;

    public ApiClient(
        QuizProperties properties,
        ResourceLoader resourceLoader,
        ObjectMapper objectMapper,
        HttpClient httpClient
    ) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public ApiResponse getMessages(String regNo, int pollIndex) throws Exception {
        if (!properties.isLiveMode()) {
            return getFixtureResponse(pollIndex);
        }

        ensureLiveBaseUrl();
        String url = stripTrailingSlash(properties.getBaseUrl())
            + "/quiz/messages?regNo="
            + URLEncoder.encode(regNo, StandardCharsets.UTF_8)
            + "&poll="
            + pollIndex;
        log.info("Polling quiz messages for poll {}", pollIndex);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), "poll", response.body());
        return objectMapper.readValue(response.body(), ApiResponse.class);
    }

    public SubmissionResponse submit(String regNo, List<LeaderboardEntry> leaderboard, int totalScore) throws Exception {
        if (!properties.isLiveMode()) {
            return SubmissionResponse.dryRun(totalScore, "Fixture mode: no external submit performed.");
        }

        if (!properties.isLiveSubmitEnabled()) {
            return SubmissionResponse.dryRun(totalScore, "Live mode is enabled, but external submit is blocked by configuration.");
        }

        ensureLiveBaseUrl();
        String url = stripTrailingSlash(properties.getBaseUrl()) + "/quiz/submit";
        log.info("Submitting leaderboard to configured live endpoint");
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("regNo", regNo);
        payload.put("leaderboard", leaderboard);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload), StandardCharsets.UTF_8))
            .timeout(Duration.ofSeconds(30))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ensureSuccess(response.statusCode(), "submit", response.body());
        SubmissionResponse parsed = objectMapper.readValue(response.body(), SubmissionResponse.class);
        return parsed.withRawBody(response.body());
    }

    private ApiResponse getFixtureResponse(int pollIndex) throws IOException {
        Map<Integer, ApiResponse> responses = fixtureCache;
        if (responses == null) {
            synchronized (this) {
                responses = fixtureCache;
                if (responses == null) {
                    responses = loadFixtureResponses();
                    fixtureCache = responses;
                }
            }
        }

        ApiResponse response = responses.get(pollIndex);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing fixture response for poll " + pollIndex);
        }
        return response;
    }

    private Map<Integer, ApiResponse> loadFixtureResponses() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getFixturePath());
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Fixture file not found: " + properties.getFixturePath());
        }
        log.info("Loading fixture responses from {}", properties.getFixturePath());

        try (InputStream inputStream = resource.getInputStream()) {
            List<ApiResponse> responses = objectMapper.readValue(inputStream, new TypeReference<List<ApiResponse>>() {});
            Map<Integer, ApiResponse> byPollIndex = new HashMap<Integer, ApiResponse>();
            for (ApiResponse response : responses) {
                byPollIndex.put(response.pollIndex(), response);
            }
            return byPollIndex;
        }
    }

    private void ensureSuccess(int statusCode, String action, String body) {
        if (statusCode >= 200 && statusCode < 300) {
            return;
        }

        throw new ResponseStatusException(
            HttpStatus.BAD_GATEWAY,
            "External " + action + " request failed with status " + statusCode + ": " + body
        );
    }

    private void ensureLiveBaseUrl() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "app.base-url must be set in live mode");
        }
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
