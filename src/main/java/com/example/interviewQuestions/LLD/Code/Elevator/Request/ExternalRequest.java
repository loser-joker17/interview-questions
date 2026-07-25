package com.example.interviewQuestions.LLD.Code.Elevator.Request;

import com.example.interviewQuestions.LLD.Code.Elevator.Request.Request;
import com.example.interviewQuestions.LLD.Code.Elevator.State.Direction;

public class ExternalRequest extends Request {
    private Direction direction;

    public ExternalRequest(int floor, Direction direction,long timeStamp){
        super(floor,timeStamp);
        this.direction=direction;
    }
    public Direction getDirection() {
        return direction;
    }
}
