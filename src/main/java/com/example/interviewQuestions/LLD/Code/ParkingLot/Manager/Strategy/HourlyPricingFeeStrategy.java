package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class HourlyPricingFeeStrategy implements FeeStrategy{

    Map<SpotType,Double> hourlyRates;

    public HourlyPricingFeeStrategy(){
        hourlyRates = new HashMap<>();
        hourlyRates.put(SpotType.SMALL,100.0);
        hourlyRates.put(SpotType.COMPACT,300.0);
        hourlyRates.put(SpotType.LARGE,500.0);
    }
    @Override
    public double calculateFee(Ticket ticket){
        LocalTime entryTime = ticket.getEntryTime();
        LocalTime exitTime = ticket.getExitTime();
        SpotType spotType = ticket.getParkingSpot().getSpotType();

        Double vehicleRate = hourlyRates.get(spotType);
        if (vehicleRate == null) {
            throw new IllegalStateException("No hourly rate configured for spot type: " + spotType);
        }
        double duration = ticket.getDuration(entryTime,exitTime);

        double fareAmount = duration*vehicleRate;
        return fareAmount;
    }
}
