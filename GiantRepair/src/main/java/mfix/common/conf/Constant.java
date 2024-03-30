package mfix.common.conf;

import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.node.match.MatchLevel;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class Constant {

    /* tool function setting */
    public static String TOOL_FUNCTION = "Repair";

    /*  basic path configurations */
    public final static String HOME = System.getProperty("user.dir");
    public final static char SEP = File.separatorChar;
    public final static String RES_DIR = HOME + SEP + "resources";
    public final static String NEW_LINE = "\n";

    /* display tags for source code diff */
    public final static String PATCH_KEEP_LEADING = " ";
    public final static String PATCH_DEL_LEADING = "-";
    public final static String PATCH_ADD_LEADING = "+";

    /* runtime configurations */
    public final static String RUNTIME_CONF_FILE = Utils.join(SEP, RES_DIR, "conf", "configure.properties");
    public static int MAX_SKELETONS;
    public static int MAX_INSTANCE_PER_SKELETON;

    /* max number of candidate patches for each subject */
    public static int MAX_PATCH_NUMBER;

    /* max number of repair time for single subject (in minutes) */
    public static int MAX_REPAIR_TIME;

    private final static String RESULT = Utils.join(SEP, HOME, "result");

    /* default directory to patch log info */
    public final static String PATCH_PATH = Utils.join(SEP, RESULT, "patch");

    /* default directory to log info */
    public final static String REPAIR_LOG_PATH = Utils.join(SEP, RESULT, "log");

    /* default home directory of pattern files */

    /* system configuration for running external command (defects4j)*/
    public static String D4J_HOME;
    public static String CMD_DEFECTS4J;

    public static String JAVA_8_HOME;
    public static String CMD_TIMEOUT;

    /* configurations for skeleton abstraction*/
    public static Boolean COMBINATION_OPTION = false;

    /* configuration for skeleton instantiation*/
    public static Boolean CONTEXT_AWARE_OPTION = true;


    public static String BUGGY_METHODS_DIR;

    public static String PATCHES_DIR;

    public final static int TEST_SUBJECT_TIMEOUT=600; //in seconds
    /*
     * Defects4j configure information
     */
    private final static String D4J_INFO_DIR = Utils.join(SEP, RES_DIR, "d4j-info");
    public final static String D4J_SRC_INFO = Utils.join(SEP, D4J_INFO_DIR, "src_path");
    public final static String D4J_PROJ_DEFAULT_HOME = Utils.join(SEP, HOME, "projects");
    public final static String D4J_PROJ_JSON_FILE = Utils.join(SEP, D4J_INFO_DIR, "project.json");

    public final static String D4J_FUNC_INFO = Utils.join(SEP, D4J_INFO_DIR, "funcinfo");


    static {
        Properties prop = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(RUNTIME_CONF_FILE));
            prop.load(in);
            // for mac
            //JAVA_8_HOME = prop.getProperty("JAVA_8_HOME", "/Library/Java/JavaVirtualMachines/jdk1.8.0_361.jdk/Contents/Home");

            // for linux
            JAVA_8_HOME = prop.getProperty("JAVA_8_HOME", "/usr/lib/jvm/java-1.8.0-openjdk-amd64");

            D4J_HOME = prop.getProperty("DEFECTS4J_HOME", "");

            CMD_DEFECTS4J = Utils.join(SEP, D4J_HOME, "framework", "bin", "defects4j");

            MAX_REPAIR_TIME = Integer.parseInt(prop.getProperty("TIMEOUT", "120"));

            MAX_SKELETONS = Integer.parseInt(prop.getProperty("MAX_SKELETON", "3"));

            MAX_INSTANCE_PER_SKELETON = Integer.parseInt(prop.getProperty("MAX_INSTANCE_PER_SKELETON", "500"));

            MAX_PATCH_NUMBER = Integer.parseInt(prop.getProperty("MAX_PATCH_NUMBER", "20"));

            /* fault localization */
            CMD_TIMEOUT = prop.getProperty("CMD.TIMEOUT", "/usr/bin/timeout");

            /* modifications apply strategy*/
            COMBINATION_OPTION = prop.getProperty("COMBINATION_OPTION", "false").equals("true");

            /* skeleton instance*/
            CONTEXT_AWARE_OPTION = prop.getProperty("CONTEXT_AWARE", "true").equals("true");

            BUGGY_METHODS_DIR = prop.getProperty("Buggy_Methods", "");

            PATCHES_DIR = prop.getProperty("Patch_Methods", "");

        } catch (IOException e) {
            LevelLogger.error("#Constant get properties failed!" + e.getMessage());
        }
    }

}
