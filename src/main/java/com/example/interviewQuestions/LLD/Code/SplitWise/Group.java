package com.example.interviewQuestions.LLD.Code.SplitWise;

import java.util.ArrayList;
import java.util.List;

public class Group {
    private int groupId;
    private String groupName;
    private final List<User> userList;

    public Group(int groupId, String groupName) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.userList = new ArrayList<>();
    }

    public void addUser(User user) { userList.add(user); }

    public int getGroupId() { return groupId; }
    public String getGroupName() { return groupName; }
    public List<User> getUserList() { return userList; }
    public void setGroupId(int groupId) { this.groupId = groupId; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    // Same note as User: override equals()/hashCode() on groupId since
    // Group is used as a map key in SplitWiseManager.shareExpenses.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Group)) return false;
        return groupId == ((Group) o).groupId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(groupId);
    }
}