package com.example.interviewQuestions.LLD.Code.Elevator;

import com.example.interviewQuestions.LLD.Code.Elevator.State.RequestState;

public abstract class Request {
    protected int floor;
    protected long timeStamp;

    protected RequestState requestState;

    public Request(int floor,long timeStamp){
        this.floor=floor;
        this.timeStamp=timeStamp;
    }

    public int getFloor() {
        return floor;
    }

    public RequestState getRequestState() {
        return requestState;
    }

    public void setRequestState(RequestState requestState) {
        this.requestState = requestState;
    }
}

