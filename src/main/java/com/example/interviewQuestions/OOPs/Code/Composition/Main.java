package com.example.interviewQuestions.OOPs.Code.Composition;

public class Main {
    public static void main(String[] args) {

        // this is without composition means there is no use of Payment class;

        BankTransfer bank = new BankTransfer();
        bank.pay();

        CardPayment cardPayment = new CardPayment();
        cardPayment.pay();

        UpiPayment upiPayment = new UpiPayment();
        upiPayment.pay();
        // But In future I have to validate or checking the fraud and save transaction every time then If we don't have a Payment service, we might do this:
//        BankTransfer bank = new BankTransfer();
//
//        validate();
//        checkFraud();
//        bank.pay();
//        saveTransaction();
//        sendEmail();
//
//        Now imagine this code exists in 20 different places.
//
//        Tomorrow you add CryptoPayment.
//
//        You'll end up copying the same workflow again:
//
//        CryptoPayment crypto = new CryptoPayment();
//
//        validate();
//        checkFraud();
//        crypto.pay();
//        saveTransaction();
//        sendEmail();  for all the suppose 20 class

        // If we have a Payment class then we just need to modify the payment
//        void pay() {
//            validate();
//            checkFraud();
//
//            processor.pay();
//
//            saveTransaction();
//            sendEmail();
//        }

        Payment payment = new Payment(new BankTransfer());
        payment.pay();

    }
}
