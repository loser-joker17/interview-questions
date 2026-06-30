package com.example.interviewQuestions.OOPs.Code.Composition;

public class Payment {
    private PaymentProcessor paymentProcessor;

    public Payment(PaymentProcessor paymentProcessor){
        this.paymentProcessor=paymentProcessor;
    }

    public void pay(){
        paymentProcessor.pay();
    }
}
