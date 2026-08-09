package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.Appender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.ConsoleAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.DatabaseAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.FileAppender;

import java.util.ArrayList;
import java.util.List;

public class AppendManager {
    private final List<Appender> appenders = new ArrayList<>();

    public void addAppender(Appender appender) {
        appenders.add(appender);
    }
    public void append(LogMessage logMessage) {
        for (Appender appender : appenders) {
            appender.append(logMessage);
        }
    }

}
