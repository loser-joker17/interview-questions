package com.example.interviewQuestions.LLD.Code.ParkingLot;

import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.ParkingSpot;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Entities.Vehicle;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.SpotType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Enums.VehicleType;
import com.example.interviewQuestions.LLD.Code.ParkingLot.Manager.ParkingSpotManager;

public class MainClient {
    //Does this method change only this object's state? then Put it inside the Entity.
    //Does this method coordinate multiple objects? then Manager / Service.
    // Does it search collections? // Manager.
    Vehicle bike = new Vehicle("UP653522",VehicleType.BIKE);
    Vehicle car = new Vehicle("MH235748",VehicleType.CAR);
    Vehicle bus = new Vehicle("MH234859",VehicleType.BUS);

    ParkingSpot bikeSpot = new ParkingSpot("101", SpotType.SMALL);
    ParkingSpot carSpot = new ParkingSpot("102",SpotType.COMPACT);
    ParkingSpot busSpot = new ParkingSpot("103",SpotType.LARGE);



}
