package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.FeeCalculator;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.TicketManager;

public class ParkingLotManager {
    private TicketManager ticketManager;
    private ParkingSpotManager parkingSpotManager;
    private FeeCalculator feeCalculator;

    public ParkingLotManager(TicketManager ticketManager,ParkingSpotManager parkingSpotManager,
                             FeeCalculator feeCalculator){
        this.ticketManager=ticketManager;
        this.parkingSpotManager=parkingSpotManager;
        this.feeCalculator=feeCalculator;
    }
    public reserveSpot(){

    }
}
