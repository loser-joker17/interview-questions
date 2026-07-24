package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.DoorState;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;
import java.util.TreeSet;

public class Elevator {
    private int elevatorId;
    private int currentFloor;
    private int capacity;
    private TreeSet<Integer> upStops;
    private TreeSet<Integer> downStops;
    private ElevatorState elevatorState;
    private DoorState doorState;
    private Direction currentDirection;

    public Elevator(int elevatorId,int currentFloor,int capacity){
        this.capacity=capacity;
        this.currentFloor=currentFloor;
        this.elevatorId=elevatorId;

        this.upStops = new TreeSet<>();
        this.downStops = new TreeSet<>((a,b)-> b-a);
        this.elevatorState = ElevatorState.IDLE;
        this.doorState = DoorState.CLOSE;
        this.currentDirection = Direction.UPWARD;
    }

    public void addStop(Request request){
        int floor = request.getFloor();

        if(floor > currentFloor){
            upStops.add(floor);
        }else if(floor < currentFloor){
            downStops.add(floor);
        }
    }
    public void moveOneStep(){
       int destination = getNextStop();

       if(destination==-1){
           elevatorState=ElevatorState.IDLE;
           return;
       }

       if(destination > currentFloor){
           currentFloor++;
       }else{
           currentFloor--;
       }

       if(destination==currentFloor){
           doorState=DoorState.OPEN;
           if(currentDirection==Direction.UPWARD){
               upStops.remove(destination);
           }
           if(currentDirection==Direction.DOWNWARD){
               downStops.remove(destination);
           }
           doorState=DoorState.CLOSE;
       }
    }
    public int getNextStop() {
        if (currentDirection == Direction.UPWARD && !upStops.isEmpty()) {
            return upStops.first();
        }

        if (currentDirection == Direction.DOWNWARD && !downStops.isEmpty()) {
            return downStops.first();
        }

        return -1;
    }
    public int getElevatorId() {
        return elevatorId;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getCurrentFloor() {
        return currentFloor;
    }

    public ElevatorState getElevatorState() {
        return elevatorState;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public DoorState getDoorState() {
        return doorState;
    }
}
