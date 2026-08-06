package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.FeeCalculator;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.TicketManager;

public class MainClient {
    public static void main(String[] args) {
        //Does this method change only this object's state? then Put it inside the Entity.
        //Does this method coordinate multiple objects? then Manager / Service.
        // Does it search collections? // Manager.
        TicketManager ticketManager = new TicketManager();
        ParkingSpotManager parkingSpotManager = new ParkingSpotManager();
        FeeCalculator feeCalculator = new FeeCalculator();

        ParkingLotManager parkingLotManager = new ParkingLotManager(ticketManager, parkingSpotManager, feeCalculator);

        Vehicle bike = new Vehicle("UP653522", VehicleType.BIKE);
        Vehicle car = new Vehicle("MH235748", VehicleType.CAR);
        Vehicle bus = new Vehicle("MH234859", VehicleType.BUS);

        Ticket bikeTicket = parkingLotManager.parkVehicle(bike);

        System.out.println("Bike Ticket Number :- " + bikeTicket.getTicketNumber());

        Ticket carTicket = parkingLotManager.parkVehicle(car);

        System.out.println("Car Ticket Number :- " + carTicket.getTicketNumber());

    }
}
