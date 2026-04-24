package com.example.quiz.model;

public record SubmissionResponse(
    Boolean isCorrect,
    Boolean isIdempotent,
    Integer submittedTotal,
    Integer expectedTotal,
    String message,
    String rawBody
) {

    public static SubmissionResponse dryRun(int submittedTotal, String message) {
        return new SubmissionResponse(null, null, submittedTotal, null, message, null);
    }

    public SubmissionResponse withRawBody(String rawBody) {
        return new SubmissionResponse(isCorrect, isIdempotent, submittedTotal, expectedTotal, message, rawBody);
    }
}

