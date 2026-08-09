package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.Appender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.ConsoleAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.DatabaseAppender;

public class AppendManager {
    private Appender appender;

    public AppendManager(Appender appender){
        this.appender=appender;
    }
    public void append(LogMessage logMessage){
        Appender consoleAppender = new ConsoleAppender();
        consoleAppender.append(logMessage);
        Appender databaseAppender = new DatabaseAppender();
        databaseAppender.append(logMessage);
        Appender fileAppender = new DatabaseAppender();
        fileAppender.append(logMessage);
    }

}
