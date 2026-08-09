package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.JsonFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.SimpleFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class ConsoleAppender implements Appender{
    private final Formatter formatter;

    public ConsoleAppender(Formatter formatter){
        this.formatter=formatter;
    }
    @Override
    public void append(LogMessage message){
        String formatted = formatter.formatMessage(message);
        System.out.println(formatted);
    }
}
