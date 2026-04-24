package com.example.quiz.service;

import com.example.quiz.client.ApiClient;
import com.example.quiz.config.QuizProperties;
import com.example.quiz.model.ApiResponse;
import com.example.quiz.model.Event;
import com.example.quiz.model.LeaderboardEntry;
import com.example.quiz.model.RunResult;
import com.example.quiz.model.SubmissionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private static final int REQUIRED_POLLS = 10;
    private static final long REQUIRED_DELAY_MILLIS = 5000L;

    private final ApiClient apiClient;
    private final QuizProperties properties;
    private final AtomicBoolean liveSubmissionPerformed = new AtomicBoolean(false);

    public QuizService(ApiClient apiClient, QuizProperties properties) {
        this.apiClient = apiClient;
        this.properties = properties;
    }

    public RunResult execute(boolean submit) throws Exception {
        return execute(submit, null);
    }

    public RunResult execute(boolean submit, String requestedRegNo) throws Exception {
        validateRuntimeConfig();
        log.info("Starting quiz workflow in {} mode", properties.getMode().name().toLowerCase());
        Map<String, Integer> scoreMap = new HashMap<String, Integer>();
        Set<String> seen = new HashSet<String>();
        String effectiveRegNo = firstNonBlank(requestedRegNo, properties.getRegNo());
        if (properties.isLiveMode() && (effectiveRegNo == null || effectiveRegNo.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "regNo is required in live mode");
        }
        int uniqueEvents = 0;

        for (int poll = 0; poll < properties.getPollCount(); poll++) {
            ApiResponse apiResponse = apiClient.getMessages(effectiveRegNo, poll);

            if (effectiveRegNo == null || effectiveRegNo.isBlank()) {
                effectiveRegNo = apiResponse.regNo();
            }

            validateResponse(apiResponse, effectiveRegNo, poll);

            for (Event event : apiResponse.events()) {
                String key = event.roundId() + "|" + event.participant();
                if (!seen.add(key)) {
                    log.debug("Duplicate event ignored: {}", key);
                    continue;
                }

                uniqueEvents++;
                scoreMap.merge(event.participant(), event.score(), Integer::sum);
            }

            if (properties.isLiveMode() && poll < properties.getPollCount() - 1) {
                Thread.sleep(properties.getDelayMillis());
            }
        }

        List<LeaderboardEntry> leaderboard = buildLeaderboard(scoreMap);
        int totalScore = leaderboard.stream().mapToInt(LeaderboardEntry::totalScore).sum();
        SubmissionResponse submissionResponse = submit
            ? submitOnce(effectiveRegNo, leaderboard, totalScore)
            : SubmissionResponse.dryRun(totalScore, "Submission skipped because submit=false");
        log.info("Workflow complete with {} unique events and total score {}", uniqueEvents, totalScore);

        return new RunResult(
            effectiveRegNo,
            properties.getMode().name().toLowerCase(),
            uniqueEvents,
            totalScore,
            leaderboard,
            submissionResponse
        );
    }

    private void validateRuntimeConfig() {
        if (properties.getPollCount() != REQUIRED_POLLS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "poll-count must be exactly " + REQUIRED_POLLS + " for this assignment"
            );
        }

        if (properties.isLiveMode() && properties.getDelayMillis() < REQUIRED_DELAY_MILLIS) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "delay-millis must be at least " + REQUIRED_DELAY_MILLIS + " in live mode"
            );
        }
    }

    private SubmissionResponse submitOnce(String regNo, List<LeaderboardEntry> leaderboard, int totalScore) throws Exception {
        if (properties.isLiveMode() && !liveSubmissionPerformed.compareAndSet(false, true)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Live submission has already been performed once for this app instance");
        }
        return apiClient.submit(regNo, leaderboard, totalScore);
    }

    private void validateResponse(ApiResponse response, String expectedRegNo, int expectedPollIndex) {
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Null response received from source");
        }

        if (response.events() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Missing events array for poll " + expectedPollIndex);
        }

        if (response.pollIndex() != expectedPollIndex) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Expected pollIndex " + expectedPollIndex + " but received " + response.pollIndex()
            );
        }

        if (properties.isLiveMode()
            && expectedRegNo != null
            && !expectedRegNo.isBlank()
            && !expectedRegNo.equals(response.regNo())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Registration number mismatch at poll " + expectedPollIndex);
        }
    }

    private List<LeaderboardEntry> buildLeaderboard(Map<String, Integer> scoreMap) {
        List<LeaderboardEntry> leaderboard = new ArrayList<LeaderboardEntry>();
        for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
            leaderboard.add(new LeaderboardEntry(entry.getKey(), entry.getValue()));
        }

        leaderboard.sort(
            Comparator.comparingInt(LeaderboardEntry::totalScore)
                .reversed()
                .thenComparing(LeaderboardEntry::participant)
        );

        return leaderboard;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second;
    }
}
