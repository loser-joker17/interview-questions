package com.example.interviewQuestions.OOPs.Code.Composition;

public class CardPayment implements PaymentProcessor{
    @Override
    public void pay(){
        System.out.println("Payment is done through Credit Card");
    }
}
