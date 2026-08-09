package com.example.interviewQuestions.LLD.Code.LoggerDesign.appender;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public interface Appender {
    public void append(LogMessage logMessage);
}
