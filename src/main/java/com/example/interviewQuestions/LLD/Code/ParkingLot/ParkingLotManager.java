package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.FeeCalculator;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.TicketManager;

import java.time.LocalTime;

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
    public Ticket parkVehicle(Vehicle vehicle){
        ParkingSpot parkingSpot = parkingSpotManager.reserveSpot(vehicle);
        Ticket ticket = ticketManager.createTickets(vehicle,parkingSpot);

        return ticket;
    }

}
