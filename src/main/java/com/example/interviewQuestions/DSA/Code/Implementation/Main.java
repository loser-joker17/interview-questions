package com.example.interviewQuestions.DSA.Code.Implementation;
import java.util.*;
import java.lang.*;

class Node{
    int key;
    int val;
    Node next;
    Node prev;

    Node(int key, int val){
        this.key=key;
        this.val=val;
        this.next=null;
        this.prev=null;
    }
}

class LRU{
    int capacity;
    Node head;
    Node tail;
    Map<Integer,Node> map;


    LRU(int capacity){
        this.capacity=capacity;
        map = new HashMap<>();

        head = new Node(-1,-1);
        tail = new Node(-1,-1);

        head.next=tail;
        tail.prev=head;
    }

    public void addToFront(Node node){
        Node temp = head.next;

        node.prev=head;
        node.next=temp;

        head.next=node;
        temp.prev=node;
    }

    public void moveToFront(Node node){
        removeNode(node);
        addToFront(node);
    }

    public void removeNode(Node node){
        Node prevNode = node.prev;
        Node nextNode = node.next;

        prevNode.next = nextNode;
        nextNode.prev=prevNode;
    }

    public Node removeTail(){
        Node lastNode = tail.prev;

        removeNode(lastNode);

        return lastNode;
    }

    public int get(int key){
        if(!map.containsKey(key)){
            return -1;
        }

        Node node = map.get(key);
        moveToFront(node);
        return node.val;
    }

    public void put(int key,int val){
        if(map.containsKey(key)){

            Node node = map.get(key);
            addToFront(node);
        }

        if(map.size() == capacity){
            Node node = removeTail();

            map.remove(node.key);
        }

        Node node = new Node(key,val);
        addToFront(node);
        map.put(key,node);

    }
}


class Main
{
    public static void main (String[] args) throws java.lang.Exception
    {
        // your code goes here

        LRU lru = new LRU(2);

        lru.put(1,1);
        lru.put(2,2);
        int getValue = lru.get(1);

        System.out.println("Get Key value :- " + getValue);
        lru.put(3,3);
        lru.put(4,4);

        getValue = lru.get(1);

        System.out.println("Get Key value :- " + getValue);


    }
}
