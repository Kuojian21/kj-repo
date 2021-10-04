package com.kj.repo.demo.args;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class TeTLArgsParse {

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("t", true, "opt");
        options.addOption("", "long-opt", true, "long-opt");
        CommandLine command = TLArgsParse.parse(options, new String[] {"-t", "z", "--long-opt", "kj"});
        if (command.hasOption("t")) {
            System.out.println(command.getOptionValue("t"));
        }
        if (command.hasOption("long-opt")) {
            System.out.println(command.getOptionValue("long-opt"));
        }
    }
}
