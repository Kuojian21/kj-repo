package com.kj.repo.test.debug;

import com.sun.tools.attach.VirtualMachine;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.DelayQueue;

public class Main {

    public static void main(String[] args) throws Exception {
        String pid = args[0];
        VirtualMachine vm = VirtualMachine.attach(pid);
        System.out.println(Main.class.getResource("Debug.class"));
        vm.loadAgent(Main.class.getResource("Debug.class").toString(), args[1]);

        Deque<Integer> deque = new LinkedList<>();
        deque.push(1);
        deque.push(2);
        System.out.println(deque.pop());
    }

}