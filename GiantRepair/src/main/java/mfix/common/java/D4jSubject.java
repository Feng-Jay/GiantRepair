

package mfix.common.java;

import mfix.common.cmd.CmdFactory;
import mfix.common.cmd.ExecuteCommand;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;

import java.util.LinkedList;
import java.util.List;

public class D4jSubject extends Subject {

    public final static String NAME = "D4jSubject";
    private List<String> _failedTestCases;


    public D4jSubject(String base, String name, int id) {
        this(base, name, id, false);
    }

    public D4jSubject(String base, String name, int id, boolean memCompile) {
        super(Utils.join(Constant.SEP, base, name, name + "_" + id + "_buggy"), name);
        _type = NAME;
        _id = id;
        setClasspath(obtainClasspath(name));
        setSourceLevel(name.equals("chart") ? SOURCE_LEVEL.L_1_8 : SOURCE_LEVEL.L_1_8);
        setCompileFile(!memCompile);
        setCompileProject(memCompile);
        setTestProject(true);
        setCompileCommand(Constant.CMD_DEFECTS4J + " compile");
        setTestCommand(Constant.CMD_DEFECTS4J + " test");
        setPath(name, id);
        setJDKHome(Constant.JAVA_8_HOME);
        setCompileSuccessMessage("BUILD SUCCESSFUL");
        setCompileFailedMessage("BUILD FAILED");
        setTestSuccessMessage("Failing tests: 0");
    }

    private void setPath(String projName, int id) {
        String file = Utils.join(Constant.SEP, Constant.D4J_SRC_INFO, projName, id + ".txt");
        List<String> paths = JavaFile.readFileToStringList(file);
        if(paths == null || paths.size() < 4) {
            LevelLogger.error(String.format("D4jSubject#setPath : path info error : <{0}>", file));
            return;
        }
        _ssrc = paths.get(0);
        _sbin = paths.get(1);
        _tsrc = paths.get(2);
        _tbin = paths.get(3);

        file = Utils.join(Constant.SEP, Constant.D4J_FUNC_INFO, projName, id+".txt");
        paths = JavaFile.readFileToStringList(file);
        if(paths == null || paths.size() < 3) {
            LevelLogger.error(String.format("D4jSubject#FuncInfo : path info error : <{0}>", file));
            return;
        }
        _javaPath = paths.get(0);
        _funcBeginLine = Integer.valueOf(paths.get(1));
        _funcEndLine   = Integer.valueOf(paths.get(2));

    }

    @Override
    public boolean test(String testcase) {
        LevelLogger.info("SINGLE TEST : " + testcase);
        return checkSuccess(ExecuteCommand.execute(CmdFactory.createCommand(getHome(),
//                Constant.CMD_TIMEOUT
//                        + " " + Constant.TEST_CASE_TIMEOUT + " " +
                         Constant.CMD_DEFECTS4J + " test -t " + testcase),
                getJDKHome()), _key_test_suc);
    }

    public boolean test(String clazz, String method) {
        return test(clazz + "::" + method);
    }



    @Override
    public String toString() {
        return "[_name=" + _name + ", " + ", _id=" + _id + ", _ssrc=" + _ssrc
                + ", _tsrc=" + _tsrc + ", _sbin=" + _sbin
                + ", _tbin=" + _tbin + "]";
    }

    private static List<String> obtainClasspath(final String projName) {
        List<String> classpath = new LinkedList<String>();
        switch (projName) {
            case "math":
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                break;
            case "chart":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/iText-2.1.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/servlet.jar");
                break;
            case "lang":
                classpath.add(Constant.D4J_LIB_DIR + "/cglib-nodep-2.2.2.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/commons-io-2.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/easymock-3.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-1.2.jar");
                break;
            case "closure":
                classpath.add(Constant.D4J_LIB_DIR + "/caja-r4314.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jarjar.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant-launcher.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/args4j.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jsr305.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/guava.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/json.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/protobuf-java.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/rhino.jar");
                break;
            case "time":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/joda-convert-1.2.jar");
                break;
            case "mockito":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/asm-all-5.0.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/assertj-core-2.1.0.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/cglib-and-asm-1.0.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/cobertura-2.0.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/fest-assert-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/fest-util-1.1.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-all-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-2.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-2.2.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/powermock-reflect-1.2.5.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/.jar");
                break;
            default:
                System.err.println("UNKNOWN project name : " + projName);
        }
        return classpath;
    }
}
