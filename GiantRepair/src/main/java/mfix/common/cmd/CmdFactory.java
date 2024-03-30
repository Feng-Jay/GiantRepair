package mfix.common.cmd;

import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.java.Subject;
public class CmdFactory {

    public static String [] createCommand(String command){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(command);
        String[] cmd = new String[]{"/bin/bash", "-c", stringBuffer.toString()};
        return cmd;
    }

    public static String[] createCommand(String dir, String command) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd " + dir);
        stringBuffer.append(" && " + command);
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createTestCommand(int timeout, Subject subject) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd ").append(subject.getHome()) // cd bugs and patches' storage directory.

//                .append(" && ").append(Constant.CMD_TIMEOUT)
//                .append(" ").append(timeout)
                .append(" && ").append(subject.getTestCommand());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createTestCommand(Subject subject) {
        return createTestCommand(Constant.TEST_SUBJECT_TIMEOUT, subject);
    }

    public static String[] createComipleCommand(Subject subject) {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd " + subject.getHome());
        stringBuffer.append(" && " + subject.getCompileCommand());
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

    public static String[] createExpressAPRCommand(Subject subject){
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("cd /data/PLM4APR/ExpressAPR");
        stringBuffer.append(" && ./expapr-cli run -t fallback -w /data/PLM4APR/tmp/exapr/")
                .append(subject.getName()).append("/")
                .append(subject.getName()).append("_").append(subject.getId()).append("_buggy/")
                .append(" \"/data/PLM4APR/GenPat/patches/*.json\"")
                .append(" && rm /data/PLM4APR/GenPat/patches/*.json");
        String[] cmd = new String[] { "/bin/bash", "-c", stringBuffer.toString() };
        return cmd;
    }

}
