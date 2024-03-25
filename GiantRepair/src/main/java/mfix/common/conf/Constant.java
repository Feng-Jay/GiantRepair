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

    /* pattern version tags */
    public static String PATTERN_VERSION;

    /* files used for pattern abstraction */
    public final static String API_TOKENS = Utils.join(SEP, RES_DIR, "db", "AllTokens_api.txt");
    public final static String TYPE_TOKENS = Utils.join(SEP, RES_DIR, "db", "AllTokens_type.txt");
    public final static String NAME_TOKENS = Utils.join(SEP, RES_DIR, "db", "AllTokens_var.txt");

    public final static String TF_IDF_TOKENS = Utils.join(SEP, RES_DIR, "db", "AllTokens.txt");
    public final static String DB_CACHE_FILE = Utils.join(SEP, RES_DIR, "db", "MethodTableElements.txt");
    public final static String DB_CACHE_FILE_WITH_TYPE = Utils.join(SEP, RES_DIR, "db", "MethodTableElementsWithType.txt");

    /* values for pattern abstraction */
    public final static int TOTAL_BUGGY_FILE_NUMBER = 1217392; // used to compute TF-IDF
    public final static int API_FREQUENCY = 100;
    public final static double TF_IDF_FREQUENCY = 0.5;
    public final static double TOKEN_FREQENCY = 0.005;

    public static boolean EXPAND_PATTERN = true;
    public static MatchLevel PATTERN_MATCH_LEVEL = MatchLevel.ALL;

    /* runtime configurations */
    public final static String DEFAULT_SUBJECT_XML = Utils.join(SEP, RES_DIR, "conf", "project.xml");
    public final static String RUNTIME_CONF_FILE = Utils.join(SEP, RES_DIR, "conf", "configure.properties");

    /* max number of concurrent threads for pattern cluster process */
    public static int MAX_CLUSTER_THREAD_NUM;

    /* max number of concurrent threads for pattern filter process */
    public static int MAX_FILTER_THREAD_NUM;

    /* max number of concurrent threads for keyword statistics */
    public static int MAX_KEYWORD_STATISTIC_THREAD_NUM;

    /* max number of candidate match instances for one pattern */
    public static int MAX_INSTANCE_PER_PATTERN;

    /* max number of candidate patches for each subject */
    public static int MAX_PATCH_NUMBER;

    /* max number of compiled patches for each location for ranking */
    public static int MAX_CONDITATE_NUMBER;

    /* max number of repair time for single subject (in minutes) */
    public static int MAX_REPAIR_TIME;

    /* max number of locations to repair */
    public static int MAX_REPAIR_LOCATION;

    /* max number of patterns used to repair one location */
    public static int TOP_K_PATTERN_EACH_LOCATION;

    /* max number of changed lines for pattern filtering */
    public static int FILTER_MAX_CHANGE_LINE;

    /* max number of modifications for pattern filtering */
    public static int FILTER_MAX_CHANGE_ACTION;

    /* filter deletion operation */
    public static boolean FILTER_DELETION;

    /* split clustering */
    public static boolean SPLIT_CLUSTER;

    /* max pattern number for each round of clustering, used only the option SPLIT_CLUSTER is true */
    public static int MAX_PATTERN_NUM_EACH_CLUSTER;

    private final static String RESULT = Utils.join(SEP, HOME, "result");

    /* default directory to patch log info */
    public final static String PATCH_PATH = Utils.join(SEP, RESULT, "patch");

    /* default directory to log info */
    public final static String REPAIR_LOG_PATH = Utils.join(SEP, RESULT, "log");

    /* default home directory of pattern files */

    /* system configuration for running external command (defects4j)*/
    public static String D4J_HOME;
    public static String CMD_DEFECTS4J;
    public static String JAVA_7_HOME;

    public static String JAVA_8_HOME;
    public static String CMD_TIMEOUT;

    /* configurations for mutation*/
    public static Boolean COMBINATION_OPTION = false;

    public final static int TEST_SUBJECT_TIMEOUT=600; //in seconds
    public final static int TEST_CASE_TIMEOUT=120; //in seconds

    /*
     * Defects4j configure information
     */
    private final static String D4J_INFO_DIR = Utils.join(SEP, RES_DIR, "d4j-info");
    public final static String D4J_LIB_DIR = Utils.join(SEP, D4J_INFO_DIR, "d4jlibs");
    public final static String D4J_FAULT_LOC = Utils.join(SEP, D4J_INFO_DIR, "location", "groundtruth");
    public final static String D4J_SRC_INFO = Utils.join(SEP, D4J_INFO_DIR, "src_path");
    public final static String D4J_FAILING_TEST = Utils.join(SEP, D4J_INFO_DIR, "failed_tests");
    public final static String D4J_PROJ_DEFAULT_HOME = Utils.join(SEP, HOME, "projects");
    public final static String D4J_PROJ_JSON_FILE = Utils.join(SEP, D4J_INFO_DIR, "project.json");

    public final static String D4J_FUNC_INFO = Utils.join(SEP, D4J_INFO_DIR, "funcinfo");

    /* configurations for sbfl */
    public final static int SBFL_TIMEOUT = 3600; //in seconds
    public final static String LOCATOR_HOME = Utils.join(SEP, HOME, "sbfl");
    public final static String COMMAND_LOCATOR = Utils.join(SEP, LOCATOR_HOME, "sbfl.sh ");
    public final static String LOCATOR_SUSP_FILE_BASE = Utils.join(SEP, LOCATOR_HOME, "ochiai");
    public final static String OCHIAI_RESULT = Utils.join(SEP, D4J_INFO_DIR, "location", "ochiai");
    public final static String PROJ_REALTIME_LOC_BASE = Utils.join(SEP,D4J_INFO_DIR, "location", "realtime");



    static {
        Properties prop = new Properties();
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(RUNTIME_CONF_FILE));
            prop.load(in);

            // System commands

            // for mac
//            JAVA_7_HOME = prop.getProperty("JAVA_7_HOME", "/Library/Java/JavaVirtualMachines/jdk1.7.0_80.jdk/Contents/Home");
//            JAVA_8_HOME = prop.getProperty("JAVA_8_HOME", "/Library/Java/JavaVirtualMachines/jdk1.8.0_361.jdk/Contents/Home");
            // for linux
            JAVA_7_HOME = prop.getProperty("JAVA_7_HOME", "/usr/lib/jvm/java-1.8.0-openjdk-amd64");
            JAVA_8_HOME = prop.getProperty("JAVA_8_HOME", "/usr/lib/jvm/java-1.8.0-openjdk-amd64");
            D4J_HOME = prop.getProperty("DEFECTS4J_HOME", "");
            CMD_DEFECTS4J = Utils.join(SEP, D4J_HOME, "framework", "bin", "defects4j");

            PATTERN_VERSION = "ver" + prop.getProperty("PATTERN.VERSION", "1");
            String number = prop.getProperty("PATTERN.NUMBER", "All");
            TOP_K_PATTERN_EACH_LOCATION = "All".equals(number) ? Integer.MAX_VALUE : Integer.parseInt(number);
            MAX_REPAIR_TIME = Integer.parseInt(prop.getProperty("REPAIR.TIME", "120"));
            MAX_INSTANCE_PER_PATTERN = Integer.parseInt(prop.getProperty("REPAIR.MATCH", "100"));
            MAX_PATCH_NUMBER = Integer.parseInt(prop.getProperty("REPAIR.PATCH", "2"));
            MAX_CONDITATE_NUMBER = Integer.parseInt(prop.getProperty("RANKING.SIZE", "10000"));
            MAX_REPAIR_LOCATION = Integer.parseInt(prop.getProperty("REPAIR.LOCATION", "100"));
            PATTERN_MATCH_LEVEL = MatchLevel.valueOf(prop.getProperty("MATCH.LEVEL", "FUZZY"));

            MAX_FILTER_THREAD_NUM = Integer.parseInt(prop.getProperty("FILTER.THREAD", "10"));
            MAX_CLUSTER_THREAD_NUM = Integer.parseInt(prop.getProperty("CLUSTER.THREAD", "10"));
            MAX_KEYWORD_STATISTIC_THREAD_NUM = Integer.parseInt(prop.getProperty("STATISTIC.THREAD", "10"));

            FILTER_MAX_CHANGE_LINE = Integer.parseInt(prop.getProperty("FILTER.LINE", "10"));
            FILTER_MAX_CHANGE_ACTION = Integer.parseInt(prop.getProperty("FILTER.ACTION", "5"));
            FILTER_DELETION = Boolean.parseBoolean(prop.getProperty("REMOVE.DELETION", "false"));

            SPLIT_CLUSTER = Boolean.parseBoolean(prop.getProperty("CLUSTER.SPLIT", "false"));
            MAX_PATTERN_NUM_EACH_CLUSTER = Integer.parseInt(prop.getProperty("CLUSTER.SIZE", "10000"));

            /* fault localization */
            CMD_TIMEOUT = prop.getProperty("CMD.TIMEOUT", "/usr/bin/timeout");

            /* mutation*/
            COMBINATION_OPTION = prop.getProperty("COMBINATION_OPTION", "false").equals("true");
        } catch (IOException e) {
            LevelLogger.error("#Constant get properties failed!" + e.getMessage());
        }
    }

}
