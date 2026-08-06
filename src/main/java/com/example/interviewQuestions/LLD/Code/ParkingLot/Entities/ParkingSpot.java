package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.concurrent.atomic.AtomicBoolean;

public class ParkingSpot {
    private String spotId;
    private SpotType spotType;
    private Vehicle vehicle;
    private AtomicBoolean isOccupied;

    public ParkingSpot(String spotId,SpotType spotType){
        this.spotId=spotId;
        this.spotType=spotType;
        this.isOccupied = new AtomicBoolean(false);
    }

    public void release(){
       this.vehicle=null;
       isOccupied.set(false);
    }
    public boolean assignVehicle(Vehicle vehicle){
        if (isOccupied.compareAndSet(false, true)) {
            this.vehicle = vehicle;
            return true;
        }
        return false;
    }
    public String getSpotId() {
        return spotId;
    }

    public SpotType getSpotType() {
        return spotType;
    }

    public AtomicBoolean getIsOccupied() {
        return isOccupied;
    }
}
