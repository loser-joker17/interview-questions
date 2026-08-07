package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy.FeeStrategy;

public class FeeCalculator {
    private FeeStrategy feeStrategy;
    public FeeCalculator(FeeStrategy feeStrategy){
        this.feeStrategy=feeStrategy;
    }

    public double calculateFee(Ticket ticket){
        return feeStrategy.calculateFee(ticket);
    }
}
