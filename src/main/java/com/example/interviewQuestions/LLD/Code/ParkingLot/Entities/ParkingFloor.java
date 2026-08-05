package com.example.interviewQuestions.LLD.Code.ParkingLot.Entities;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingFloor {
    private int floorNumber;
    private Map<SpotType, List<ParkingSpot>> spots;

    public ParkingFloor(int floorNumber){
        this.floorNumber=floorNumber;
        this.spots=new HashMap<>();
    }

    public ParkingSpot findAvailableSpot(){

    }
    public int getFloorNumber() {
        return floorNumber;
    }
    public Map<SpotType, List<ParkingSpot>> getSpots() {
        return spots;
    }
}
