package com.example.interviewQuestions.LLD.Code.ParkingLot.Manager;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingFloor;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;

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

    public ParkingSpot findSpot(Vehicle vehicle){
        for(ParkingFloor floor : floors){
            ParkingSpot spot = floor.findAvailableSpot();

            if(spot!=null){
                return spot;
            }
        }
        return null;
    }
}
