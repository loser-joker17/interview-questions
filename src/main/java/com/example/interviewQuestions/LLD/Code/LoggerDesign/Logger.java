package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;

import java.time.LocalDateTime;

public class Logger {
    private LevelType threshold;
    private AppendManager appendManager;

    public Logger(LevelType threshold, AppendManager appendManager){
        this.threshold=threshold;
        this.appendManager = appendManager;
    }

    public void log(LevelType level, String message){
        if(level.getSeverity()<threshold.getSeverity()){
            return;
        }
        LogMessage logMessage = new LogMessage(level,message, LocalDateTime.now());
        appendManager.append(logMessage);
    }

}
