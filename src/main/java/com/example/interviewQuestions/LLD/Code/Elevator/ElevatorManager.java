package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.Request.ExternalRequest;
import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;
import com.example.interviewQuestions.LLD.Code.Elevator.State.RequestState;

import java.util.ArrayList;
import java.util.List;

public class ElevatorManager {
    private List<Elevator> elevators;

    public ElevatorManager(){
        this.elevators = new ArrayList<>();
    }

    public void addElevator(Elevator elevator){
        elevators.add(elevator);
    }

    public List<Elevator> getElevators() {
        return elevators;
    }

    public Elevator assignRequest(ExternalRequest request){
//        int floor = request.getFloor();
//        Direction direction = request.getDirection();
        Elevator bestElevator = null;

        int bestCost = Integer.MAX_VALUE;;
        for(Elevator elevator : elevators){

            int cost = computeCost(elevator,request);

            if(bestCost > cost){
                bestCost = cost;
                bestElevator = elevator;
            }
        }

        if(bestElevator!=null){
            bestElevator.addStop(request.getFloor());
            request.setRequestState(RequestState.PENDING);
        }
        return bestElevator;
    }

    private int computeCost(Elevator e, ExternalRequest request) {
        int distance = Math.abs(e.getCurrentFloor() - request.getFloor());

        if (e.getElevatorState() == ElevatorState.IDLE) {
            return distance;
        }

        boolean sameDirection = e.getCurrentDirection() == request.getDirection();
        boolean isAhead = request.getDirection() == Direction.UPWARD
                ? request.getFloor() >= e.getCurrentFloor()
                : request.getFloor() <= e.getCurrentFloor();

        if (sameDirection && isAhead) {
            return distance; // on the way — best case
        }

        // Opposite direction or already passed: elevator must finish its
        // current sweep, reverse, then come back. Penalize accordingly.
        return distance + 1000;
    }
}
