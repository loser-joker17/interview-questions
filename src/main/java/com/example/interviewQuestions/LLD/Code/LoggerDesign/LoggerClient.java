package com.example.interviewQuestions.LLD.Code.LoggerDesign;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Enums.LevelType;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.JsonFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.SimpleFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.Appender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.ConsoleAppender;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.appender.FileAppender;

public class LoggerClient {
    public static void main(String[] args){

        Formatter simpleFormatter = new SimpleFormatter();
        Formatter jsonFormatter = new JsonFormatter();
        AppendManager appendManager = new AppendManager();
        appendManager.addAppender(new ConsoleAppender(simpleFormatter));
        appendManager.addAppender(new FileAppender(jsonFormatter));

        Logger logger = new Logger(LevelType.INFO,appendManager);

        logger.log(LevelType.INFO,"User Id is 123");
    }
}
