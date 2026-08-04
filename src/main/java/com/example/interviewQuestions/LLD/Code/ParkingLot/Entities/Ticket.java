package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import java.time.Duration;
import java.time.LocalTime;

public class Ticket {
    private final String ticketNumber;
    private ParkingSpot parkingSpot;
    private Vehicle vehicle;
    private LocalTime entryTime;
    private LocalTime exitTime;

    public Ticket(String ticketNumber,LocalTime entryTime,LocalTime exitTime){
        this.ticketNumber=ticketNumber;
        this.entryTime=entryTime;
        this.exitTime=exitTime;
    }

    public double getDuration(LocalTime entryTime,LocalTime exitTime){
        return Duration.between(entryTime,exitTime).toHours()/(60.0);
    }

    public String getTicketNumber() {
        return ticketNumber;
    }
}
