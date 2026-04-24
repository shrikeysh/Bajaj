package com.example.quiz.controller;

import com.example.quiz.model.RunResult;
import com.example.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping("/run")
    public ResponseEntity<RunResult> run(
        @RequestParam(defaultValue = "true") boolean submit,
        @RequestParam(required = false) String regNo
    ) throws Exception {
        return ResponseEntity.ok(quizService.execute(submit, regNo));
    }
}
