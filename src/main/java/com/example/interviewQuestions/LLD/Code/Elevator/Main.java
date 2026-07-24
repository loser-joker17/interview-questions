package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;
import com.example.interviewQuestions.LLD.Code.Elevator.State.ElevatorState;

public class Main {
    public static void main(String[] args) {
        ElevatorManager manager = new ElevatorManager();

        manager.addElevator(new Elevator(1, 2, 5));
        manager.addElevator(new Elevator(2, 10, 5));
        manager.addElevator(new Elevator(3, 6, 5));
        manager.addElevator(new Elevator(4, 15, 5));

        // External request: floor 6 presses UP
        ExternalRequest req1 = new ExternalRequest(6, Direction.UPWARD, System.currentTimeMillis());
        Elevator assigned = manager.assignRequest(req1);
        System.out.println("Request for floor 6 (UP) assigned to Elevator " + (assigned != null ? assigned.getElevatorId() : "NONE"));

        // External request: floor 9 presses DOWN
        ExternalRequest req2 = new ExternalRequest(9, Direction.DOWNWARD, System.currentTimeMillis());
        Elevator assigned2 = manager.assignRequest(req2);
        System.out.println("Request for floor 9 (DOWN) assigned to Elevator " + (assigned2 != null ? assigned2.getElevatorId() : "NONE"));

        // Internal request: passenger inside "assigned" elevator presses floor 12
        if (assigned != null) {
            InternalRequest internalReq = new InternalRequest(12, System.currentTimeMillis());
            assigned.addStop(internalReq);
            System.out.println("Passenger inside Elevator " + assigned.getElevatorId() + " requested floor " + internalReq.getFloor());
        }

        System.out.println("\n--- Simulating movement ---");
        for (Elevator e : manager.getElevators()) {
            simulate(e);
        }
    }

    private static void simulate(Elevator elevator) {
        int safetyLimit = 50; // guards against infinite loop bugs during testing
        while (safetyLimit-- > 0) {
            int before = elevator.getCurrentFloor();
            elevator.moveOneStep();

            if (elevator.getElevatorState() == ElevatorState.IDLE) {
                System.out.println("Elevator " + elevator.getElevatorId() + " is now IDLE at floor " + elevator.getCurrentFloor());
                break;
            }

            if (elevator.getCurrentFloor() != before) {
                System.out.println("Elevator " + elevator.getElevatorId() + " moved to floor " + elevator.getCurrentFloor()
                        + " [" + elevator.getDoorState() + "]");
            }
        }
    }
}