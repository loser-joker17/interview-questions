package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;

public class Vehicle {
    private final String vehicleId;
    private VehicleType vehicleType;

    public Vehicle(String vehicleId,VehicleType vehicleType){
        this.vehicleId=vehicleId;
        this.vehicleType=vehicleType;
    }
    public String getVehicleId() {
        return vehicleId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }
}
