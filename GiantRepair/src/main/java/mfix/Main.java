/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 */

package mfix;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.tools.Repair;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
//            System.err.println("Please given the arguments");
//            System.err.println("\tclean : delete serialized pattern files.");
//            System.err.println("\tprint : print detail information of given pattern.");
            System.err.println("\trepair : repair bugs.");
//            System.err.println("\tfilter : serialize and filter patterns and output pattern records.");
//            System.err.println("\tcluster : cluster serialized patterns.");
//            System.err.println("\tstatistic : keyword statistics.");
//            System.err.println("\tsearch : search desired patterns.");
            System.exit(1);
        }

        switch (args[0]) {
//            case "clean":
//                Cleaner cleaner = new Cleaner();
//                cleaner.clean(args);
//                break;
//            case "print":
//                PatternPrinter patternPrinter = new PatternPrinter();
//                patternPrinter.print(args);
//                break;
//            case "filter":
//                Filter filter = new Filter();
//                filter.filter(args);
//                break;
//            case "cluster":
//                Cluster cluster = new Cluster();
//                cluster.cluster(args);
//                break;
            case "diff":
                Constant.TOOL_FUNCTION = "Diff";
                Repair.diffAPI(args);
                System.exit(0);
                break;
//            case "search":
//                Search.search(args);
//                break;
            default:
                Repair.repairAPI(args);
        }
    }
}

