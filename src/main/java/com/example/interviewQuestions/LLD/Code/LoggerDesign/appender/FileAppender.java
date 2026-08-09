package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.Formatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.JsonFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter.SimpleFormatter;
import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class FileAppender implements Appender{
    private final Formatter formatter;
    public FileAppender(Formatter formatter){
        this.formatter=formatter;
    }
    @Override
    public void append(LogMessage message){
        String formatted = formatter.formatMessage(message);
        System.out.println(formatted);
    }
}
