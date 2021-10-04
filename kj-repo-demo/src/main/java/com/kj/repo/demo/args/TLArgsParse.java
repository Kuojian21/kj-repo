package com.kj.repo.demo.args;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * @author kj
 */
public class TLArgsParse {

    public static CommandLine parse(Options options, String[] args) throws ParseException {
        return new DefaultParser().parse(options, args);
    }

}
