

package mfix.common.java;

import mfix.common.cmd.CmdFactory;
import mfix.common.cmd.ExecuteCommand;
import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;

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
}
