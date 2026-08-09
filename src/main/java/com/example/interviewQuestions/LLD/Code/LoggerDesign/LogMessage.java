package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;

import java.time.LocalDateTime;

public class LogMessage {
    private final LevelType level;
    private final String message;
    private final LocalDateTime timestamp;

    public LogMessage(LevelType level, String message, LocalDateTime timestamp) {
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
    }

    public LevelType getLevel() { return level; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
