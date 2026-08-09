package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class SimpleFormatter implements Formatter {
    @Override
    public String formatMessage(LogMessage logMessage) {
        return "[" + logMessage.getTimestamp() + "] "
                + logMessage.getLevel() + " - "
                + logMessage.getMessage();
    }
}