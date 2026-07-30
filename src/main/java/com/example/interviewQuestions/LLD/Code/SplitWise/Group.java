package com.example.interviewQuestions.LLD.Code.SplitWise;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private int groupId;
    private String groupName;
    private final List<User> userList;

    public int getGroupId() {
        return groupId;
    }
    public Group(int groupId,String groupName){
        this.groupId=groupId;
        this.groupName=groupName;
        this.userList = new ArrayList<>();
    }
    public String getGroupName() {
        return groupName;
    }

    public List<User> getUserList() {
        return userList;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
}
