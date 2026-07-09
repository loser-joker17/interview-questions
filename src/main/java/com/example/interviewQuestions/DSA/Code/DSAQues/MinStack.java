package com.example.interviewQuestions.DSA.Code.DSAQues;

import java.util.Stack;

class StackMin{
    Stack<int[]> st;

    StackMin(){
        st = new Stack();
    }

    public void push(int x){
        int mini;
        if(st.isEmpty()){
            mini=x;
        }else{
            mini=Math.min(x,st.peek()[1]);
        }
        st.push(new int[]{x,mini});

    }

    public void pop(){
        if(!st.isEmpty()){
            st.pop();
        }
    }

    public int getMin(){
        if(!st.isEmpty()){
            return st.peek()[1];
        }
        return -1;
    }

    public int peek(){
        if(!st.isEmpty()){
            return st.peek()[0];
        }
        return -1;
    }
}

class MinStack
{
    public static void main (String[] args) throws java.lang.Exception
    {
        // your code goes here
        StackMin stackMin = new StackMin();

        stackMin.push(100);
        stackMin.push(10);
        stackMin.push(20);
        System.out.println(stackMin.getMin());
        stackMin.pop();
        System.out.println(stackMin.getMin());

    }
}
