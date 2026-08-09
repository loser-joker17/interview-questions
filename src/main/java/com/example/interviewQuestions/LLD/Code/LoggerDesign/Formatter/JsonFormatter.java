package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public class JsonFormatter implements Formatter {
    @Override
    public String formatMessage(LogMessage logMessage) {
        return "{"
                + "\"timestamp\":\"" + logMessage.getTimestamp() + "\","
                + "\"level\":\"" + logMessage.getLevel() + "\","
                + "\"message\":\"" + logMessage.getMessage() + "\""
                + "}";
    }
}
