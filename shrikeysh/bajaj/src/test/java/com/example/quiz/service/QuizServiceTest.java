package com.example.quiz.service;

import com.example.quiz.model.RunResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class QuizServiceTest {

    @Autowired
    private QuizService quizService;

    @Test
    void executesFixtureWorkflow() throws Exception {
        RunResult result = quizService.execute(false);

        assertNotNull(result);
        assertEquals("fixture", result.mode());
        assertEquals(9, result.uniqueEvents());
        assertEquals(210, result.totalScore());
        assertEquals(3, result.leaderboard().size());
        assertEquals("Charlie", result.leaderboard().get(0).participant());
        assertEquals(80, result.leaderboard().get(0).totalScore());
        assertNotNull(result.submissionResponse());
        assertEquals("Submission skipped because submit=false", result.submissionResponse().message());
    }
}
