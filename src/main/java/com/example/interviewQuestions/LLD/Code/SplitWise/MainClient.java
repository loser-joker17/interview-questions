package com.example.interviewQuestions.LLD.Code.SplitWise;

public class MainClient {
    public static void main(String[] args) {
        User vijay = new User(1,"Vijay");
        User ravi = new User(2,"Ravi");
        User akshat = new User(3,"Akshat");
        User satyam = new User(4,"Satyam");

        Group group1 = new Group(1,"Group1");
        group1.addUser(vijay);
        group1.addUser(ravi);
        group1.addUser(akshat);

        Group group2 = new Group(2,"group2");
        group2.addUser(satyam);
        group2.addUser(vijay);
        group2.addUser(ravi);


    }
}
