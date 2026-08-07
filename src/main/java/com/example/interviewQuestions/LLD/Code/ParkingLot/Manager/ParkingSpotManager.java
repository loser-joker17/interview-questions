package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingFloor;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;

import java.util.ArrayList;
import java.util.List;


public class ParkingSpotManager {
    private List<ParkingFloor> floors;

    public ParkingSpotManager(){
        this.floors=new ArrayList<>();
    }
    public void addParkingFloor(ParkingFloor parkingFloor){
        floors.add(parkingFloor);
    }

    public ParkingSpot reserveSpot(Vehicle vehicle){
        SpotType spotType = getRequiredSpotType(vehicle);
        for(ParkingFloor floor : floors){
            ParkingSpot spot = floor.findAvailableSpot(spotType);

            if(spot!=null){
                return spot;
            }
        }
        System.out.println();
        return null;
    }
    public void releaseSpot(ParkingSpot spot){
        spot.release();
    }
    private SpotType getRequiredSpotType(Vehicle vehicle) {
        switch(vehicle.getVehicleType()) {
            case BIKE:
                return SpotType.SMALL;
            case CAR:
                return SpotType.COMPACT;
            case BUS:
                return SpotType.LARGE;
        }
        return null;
    }
}
