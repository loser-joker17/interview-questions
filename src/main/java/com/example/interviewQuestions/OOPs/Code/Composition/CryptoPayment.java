package com.example.interviewQuestions.OOPs.Code.Composition;

public class CryptoPayment implements PaymentProcessor{
    @Override
    public void pay(){
        System.out.println("Payment done, Checking the composition behaviour ");
    }
}
