package com.example.quiz.model;

import java.util.List;

public record ApiResponse(String regNo, String setId, int pollIndex, List<Event> events) {
}

