package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class TicketManager {
    private Map<String,Ticket> tickets;
    public TicketManager(){
        this.tickets=new HashMap<>();
    }
    public Ticket createTickets(Vehicle vehicle,Ticket ticket){
        String ticketId = ticket.getTicketNumber();
        Ticket ticket1 = new Ticket()
        tickets.put(ticketId,ticket);
    }
    public Ticket findTickets(String ticketId){
        Ticket ticket = tickets.get(ticketId);
        return ticket;
    }
}
