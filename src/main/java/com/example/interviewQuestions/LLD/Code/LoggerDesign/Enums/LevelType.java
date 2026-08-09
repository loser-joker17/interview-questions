package com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums;

enum LevelType {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);
    private final int severity;
    LevelType(int severity) {
        this.severity = severity;
    }
    public int getSeverity() {
        return severity;
    }
}
