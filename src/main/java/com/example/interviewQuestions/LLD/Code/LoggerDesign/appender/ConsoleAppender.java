package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class ConsoleAppender implements Appender{
    @Override
    public void append(LogMessage message){
        System.out.println("Message added to console" + message);
    }
}
