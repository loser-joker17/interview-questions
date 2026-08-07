package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;

public class FlatPriceStrategy implements FeeStrategy{
    @Override
    public double calculateFee(Ticket ticket){
        return 400.0;
    }
}
