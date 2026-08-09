package com.example.interviewQuestions.LLD.Code.LoggerDesign.Formatter;

import com.example.interviewQuestions.LLD.Code.LoggerDesign.LogMessage;

public interface Formatter {
    public String formatMessage(LogMessage logMessage);
}
