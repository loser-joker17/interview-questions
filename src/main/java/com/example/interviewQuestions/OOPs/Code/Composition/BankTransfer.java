package com.example.interviewQuestions.OOPs.Code.Composition;

public class BankTransfer implements PaymentProcessor{
    @Override
    public void pay(){
        System.out.println("Payment is done through Bank Transfer");
    }
}
