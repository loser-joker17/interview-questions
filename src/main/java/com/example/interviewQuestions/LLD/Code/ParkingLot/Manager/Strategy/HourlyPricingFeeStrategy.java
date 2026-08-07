package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.Strategy;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Ticket;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class HourlyPricingFeeStrategy implements FeeStrategy{

    Map<VehicleType,Double> hourlyRates;

    public HourlyPricingFeeStrategy(){
        hourlyRates = new HashMap<>();
        hourlyRates.put(VehicleType.BIKE,100.0);
        hourlyRates.put(VehicleType.CAR,300.0);
        hourlyRates.put(VehicleType.BUS,500.0);
    }
    @Override
    public double calculateFee(Ticket ticket){
        LocalTime entryTime = ticket.getEntryTime();
        LocalTime exitTime = ticket.getExitTime();
        VehicleType vehicleType = ticket.getVehicle().getVehicleType();

        double vehicleRate = hourlyRates.get(vehicleType);
        double duration = ticket.getDuration(entryTime,exitTime);

        double fareAmount = duration*vehicleRate;
        return fareAmount;
    }
}
