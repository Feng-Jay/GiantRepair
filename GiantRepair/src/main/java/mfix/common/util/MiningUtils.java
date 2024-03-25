
package mfix.common.util;

import mfix.common.conf.Constant;

public class MiningUtils {

    public final static String buggyFileSubDirName() {
        return "buggy-version";
    }

    public final static String fixedFileSubDirName() {
        return "fixed-version";
    }

    public final static String patternSubDirName() {
        return "pattern-" + Constant.PATTERN_VERSION + "-serial";
    }

}
