package com.example.quiz.model;

import java.util.List;

public record RunResult(
    String regNo,
    String mode,
    int uniqueEvents,
    int totalScore,
    List<LeaderboardEntry> leaderboard,
    SubmissionResponse submissionResponse
) {
}

