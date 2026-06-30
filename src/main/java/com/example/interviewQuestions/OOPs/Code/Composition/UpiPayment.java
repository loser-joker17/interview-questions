package com.example.interviewQuestions.OOPs.Code.Composition;

public class UpiPayment implements PaymentProcessor{
    @Override
    public void pay(){
        System.out.println("Payment is done through UPI Transfer ");
    }
}
