package mfix;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.tools.Repair;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.err.println("\trepair : repair bugs.");
            System.err.println("\tdiff : extract diff scripts.");
            System.exit(1);
        }

        if (args[0].equals("diff")) {
            Constant.TOOL_FUNCTION = "Diff";
            Repair.diffAPI(args);
            System.exit(0);
        } else {
            Repair.repairAPI(args);
        }
    }
}

