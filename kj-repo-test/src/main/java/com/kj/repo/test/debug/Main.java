package com.kj.repo.test.debug;

import com.sun.tools.attach.VirtualMachine;

public class Main {

    public static void main(String[] args) throws Exception {
        String pid = args[0];
        VirtualMachine vm = VirtualMachine.attach(pid);
        System.out.println(Main.class.getResource("Debug.class"));
        vm.loadAgent(Main.class.getResource("Debug.class").toString(), args[1]);
    }

}