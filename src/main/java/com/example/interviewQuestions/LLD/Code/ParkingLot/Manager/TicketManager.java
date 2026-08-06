package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TicketManager {
    private Map<String,Ticket> activeTickets;
    public TicketManager(){
        this.activeTickets=new HashMap<>();
    }
    public Ticket createTickets(Vehicle vehicle,ParkingSpot parkingSpot){
        String ticketId = UUID.randomUUID().toString();
        Ticket ticket = new Ticket(ticketId, LocalTime.now(),null ,parkingSpot,vehicle);
        activeTickets.put(ticketId,ticket);
        return ticket;
    }
    public Ticket findTickets(String ticketId){
        Ticket ticket = activeTickets.get(ticketId);
        return ticket;
    }
}
