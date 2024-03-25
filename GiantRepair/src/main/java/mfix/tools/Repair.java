

package mfix.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import mfix.common.cmd.CmdFactory;
import mfix.common.cmd.ExecuteCommand;
import mfix.common.conf.Constant;
import mfix.common.java.D4jSubject;
import mfix.common.java.JCompiler;
import mfix.common.java.Subject;
import mfix.common.util.*;
import mfix.core.node.ast.Node;
import mfix.core.node.check.StaticCheck;
import mfix.core.node.diff.gitDiff;
import mfix.core.node.match.NodeMatcher;
import mfix.core.node.match.metric.LevenShteinDistance;
import mfix.core.node.modify.Apply;
import mfix.core.node.modify.MyActions;
import mfix.core.node.parser.StmtParser;
import org.apache.commons.cli.*;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class Repair {

    private Subject _subject;
    private String _logfile;
    private String _patchFile;
    private int _patchNum;

    private int _validPatchRank;
    private Set<String> _patternRecords;
    private String _singlePattern;

    private String _llmName;

    private Timer _timer;

    private Set<String> _alreadyGenerated = new HashSet<>();

    private Set<String> _allFailedTests = new HashSet<>();
    private Set<String> _alreadyFixedTests = new HashSet<>();
    private List<String> _currentFailedTests = new ArrayList<>();

    private Set<String> _generatedPatches = new HashSet<>();

    private int _allTestedPatches;

    private int _compileFailedPatches;

    private int _currentPatchId;

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");

    public Repair(Subject subject, Set<String> patternRecords, String singlePattern) {
        _subject = subject;
        _patchNum = 0;
        _validPatchRank = 0;
        _patternRecords = patternRecords;
        _singlePattern = singlePattern;
        _patchFile = _subject.getPatchFile();
        _logfile = _subject.getLogFile();
        _timer = new Timer(Constant.MAX_REPAIR_TIME);
        _allTestedPatches = 0;
        _compileFailedPatches = 0;
    }

    public int patch() {
        return _patchNum;
    }

    private boolean shouldStop() {
        if(_patchNum >= Constant.MAX_PATCH_NUMBER){
            JavaFile.writeStringToFile(_logfile, "Patch number exceeds the limit!\n", true);
            JavaFile.writeStringToFile(_logfile, "Tested " + _allTestedPatches + " patches\n", true);
            JavaFile.writeStringToFile(_logfile, "Compile failed " + _compileFailedPatches + " patches\n", true);
            double compilationRate = (_allTestedPatches - _compileFailedPatches)*1.0/_allTestedPatches;
            JavaFile.writeStringToFile(_logfile, "Compilation rate" + compilationRate + "\n", true);
            LevelLogger.info("Patch number exceeds the limit!");
            return true;
        }else if(_timer.timeout()){
            JavaFile.writeStringToFile(_logfile, "Time exceeds the limit!\n", true);
            LevelLogger.info("Time exceeds the limit!"+_patchNum+" patches have been generated!");
            return true;
        }
        return false;
    }

    protected void setTimer(Timer timer) {
        _timer = timer;
    }

    private enum ValidateResult{
        COMPILE_FAILED,
        TEST_FAILED,
        PASS
    }

    private ValidateResult compileValid(String clazzName, String source) {
        if (_subject.compileFile()) {
            LevelLogger.debug("Compile single file : " + clazzName);
            boolean compile = new JCompiler().compile(_subject, clazzName, source);
            if (!compile) {
                LevelLogger.debug("Compiling single file failed!");
                return ValidateResult.COMPILE_FAILED;
            }
            LevelLogger.debug("Compiling single file success!");
        }
        if (_subject.compileProject()){
            LevelLogger.debug("Compile subject : " + _subject.getName());
            boolean compile = _subject.compile();
            if (!compile){
                LevelLogger.debug("Compiling subject failed!");
                return ValidateResult.COMPILE_FAILED;
            }
            LevelLogger.debug("Compiling subject success!");
        }
        return ValidateResult.PASS;
    }

    private ValidateResult testValid() {

        //test failed classes
        boolean delete = false;
        for (String s : _allFailedTests) {
            Utils.deleteDirs(_subject.getHome() + _subject.getTbin());
            Utils.deleteDirs(_subject.getHome() + _subject.getSbin());
            delete = true;
            if (!_subject.test(s)) {
                return ValidateResult.TEST_FAILED;
            }
        }
        if (delete) {
            Utils.deleteDirs(_subject.getHome() + _subject.getTbin());
            Utils.deleteDirs(_subject.getHome() + _subject.getSbin());
        }

        // test whole project
        if (_subject.testProject()) {
            LevelLogger.debug("Test project : " + _subject.getName());
            boolean test = _subject.test();
            if (!test) {
                LevelLogger.debug("Testing project failed!");
                return ValidateResult.TEST_FAILED;
            }
            LevelLogger.debug("Testing project success!");
        }

        return ValidateResult.PASS;
    }

    private void writeLog(String patchFunc, String buggyFile, boolean patch, int counter) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("FILE : ").append(buggyFile).append(Constant.NEW_LINE)
                .append(patch ? "SUCCESS" : "FAILED").append(Constant.NEW_LINE)
                .append(patch ? patchFunc : "").append(Constant.NEW_LINE)
                .append(counter).append(" th patch/").append(_validPatchRank).append(Constant.NEW_LINE)
                .append("START : ").append(Constant.NEW_LINE)
                .append(simpleDateFormat.format(new Date(_timer.whenStart())))
                .append(Constant.NEW_LINE)
                .append("---------")
                .append("TIME : ").append(Constant.NEW_LINE)
                .append(simpleDateFormat.format(new Date()))
                .append(Constant.NEW_LINE)
                .append("--------------- END -----------------")
                .append(Constant.NEW_LINE);
        JavaFile.writeStringToFile(_logfile, buffer.toString(), true);

    }

    private void writeLog(String patchFunc, String buggyFile, boolean patch, int counter, String trace) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("FILE : ").append(buggyFile).append(Constant.NEW_LINE)
                .append(patch ? "SUCCESS" : "FAILED").append(Constant.NEW_LINE)
                .append(patch ? patchFunc : "").append(Constant.NEW_LINE)
                .append(counter).append(" th patch/").append(_validPatchRank).append(Constant.NEW_LINE)
                .append("Trace:").append(Constant.NEW_LINE)
                .append(trace).append(Constant.NEW_LINE)
                .append("START : ").append(Constant.NEW_LINE)
                .append(simpleDateFormat.format(new Date(_timer.whenStart())))
                .append(Constant.NEW_LINE)
                .append("---------")
                .append("TIME : ").append(Constant.NEW_LINE)
                .append(simpleDateFormat.format(new Date()))
                .append(Constant.NEW_LINE)
                .append("--------------- END -----------------")
                .append(Constant.NEW_LINE);
        JavaFile.writeStringToFile(_logfile, buffer.toString(), true);

    }

    private static void printProgress(int current, int total) {
        int width = 50; // progress bar width in chars

        System.out.print("\r[");
        int progress = (int) ((current / (double) total) * width);
        for (int i = 0; i < progress; i++) {
            System.out.print("=");
        }
        for (int i = progress; i < width; i++) {
            System.out.print(" ");
        }
        System.out.print("] " + current + "/" + total+";" + (int) ((current / (double) total) * 100) + "%");
//        System.out.print("] " + (int) ((current / (double) total) * 100) + "%");

        if (current == total) {
            System.out.println(); // Print a newline when done
        }
    }

    private List<Pair<String, Double>> rankPatches(List<String> patches, String buggyMethod){
        LevelLogger.debug("Start ranking "+patches.size()+" patches!");
        List<String> buggyMethodContentList = JavaFile.readFileToStringList(buggyMethod);
        StringBuilder buggyContentBuilder = new StringBuilder();
        for(int i = 0; i < buggyMethodContentList.size(); ++i){
            if(i == 0 || i == buggyMethodContentList.size()-1){
                continue;
            }else{
                buggyContentBuilder.append(buggyMethodContentList.get(i)).append("\n");
            }
        }
        String buggyContent = buggyContentBuilder.toString();

        List<Pair<Integer, Double>> patchRank = new ArrayList<>();
        for(String patch: patches){
            LevenShteinDistance calculator = new LevenShteinDistance(buggyContent, patch);
            Double distance = calculator.compute()*1.0;
            patchRank.add(new Pair<>(patches.indexOf(patch), distance));
            printProgress(patches.indexOf(patch), patches.size());
        }

        patchRank.sort(new Comparator<Pair<Integer, Double>>() {
            @Override
            public int compare(Pair<Integer, Double> o1, Pair<Integer, Double> o2) {
                return o1.getSecond().compareTo(o2.getSecond());
            }
        });
        int maxPatchRank = 500;
        List<Pair<String, Double>> rankedPatches = new ArrayList<>();
        for(Pair<Integer, Double> tuple: patchRank){
            rankedPatches.add(new Pair<>(patches.get(tuple.getFirst()), tuple.getSecond()));
            if(rankedPatches.size() >= maxPatchRank) break;
        }
        return rankedPatches;
    }

    private List<Pair<String, Double>> rankPatches(List<Pair<String, Double>> patches, int num){
        patches.sort(new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return o1.getSecond().compareTo(o2.getSecond());
            }
        });
        int maxPatchRank = 1000;
        List<Pair<String, Double>> rankedPatches = new ArrayList<>();
        for(Pair<String, Double> tuple: patches){
            if(patches.size() > 100 && num >= 30 && tuple.getSecond() > 0 && tuple.getSecond() <= 0.1) continue;
            rankedPatches.add(new Pair<>(tuple.getFirst(), tuple.getSecond()));
            if(rankedPatches.size() >= maxPatchRank) break;
        }
        return rankedPatches;
    }

    private List<Triple<String, Double, String>> rankPatches1(List<Triple<String, Double, String>> patches, int num){
        patches.sort(new Comparator<Triple<String, Double, String>>() {
            @Override
            public int compare(Triple<String, Double, String> o1, Triple<String, Double, String> o2) {
                return o1.getSecond().compareTo(o2.getSecond());
            }
        });
        int maxPatchRank = 1000;
        List<Triple<String, Double, String>> rankedPatches = new ArrayList<>();
        for(Triple<String, Double, String> tuple: patches){
            if(patches.size() > 100 && num >= 30 && tuple.getSecond() > 0 && tuple.getSecond() <= 0.1) continue;
//            if(_generatedPatches.contains(tuple.getFirst())){
//                continue;
//            }else{
//                _generatedPatches.add(tuple.getFirst());
//            }
            rankedPatches.add(new Triple<>(tuple.getFirst(), tuple.getSecond(), tuple.getThird()));
            if(rankedPatches.size() >= maxPatchRank) break;
        }
        return rankedPatches;
    }

    public List<String> preprocessFunction(List<String> pre, List<String> oricontent){
        int counter = 0;
        for(int i = 0; i < oricontent.size(); ++i){
            String tmpLine = oricontent.get(i).strip();
            tmpLine = tmpLine.replaceAll(" ", "");
            counter ++;
            if(tmpLine.endsWith("{")){
                break;
            }
        }
        for(int i = 0; i < counter; ++i){
            pre.add(oricontent.get(i));
        }
        for (int i = counter; i < oricontent.size(); ++i){
            String tmpLine = oricontent.get(i).strip();
            tmpLine = tmpLine.replaceAll(" ", "");
            if(!tmpLine.startsWith("//")){
                break;
            }else{
                counter++;
            }
        }

        Iterator<String> iterator = oricontent.iterator();
        int current = 0;
        while (iterator.hasNext() && current < counter) {
            iterator.next(); // Move to the next element
            iterator.remove(); // Remove the element
            current++;
        }

        String retPre = String.join("\n", pre);
        String retOri = String.join("\n", oricontent);
        List<String> ret = new ArrayList<>();
        ret.add(retPre);
        ret.add(retOri);
        return ret;
    }

    public String preprocessFunction(String patch){
        List<String> lines = Arrays.asList(patch.split("\n"));
        List<String> retList = new ArrayList<>();
        int counter = 0;
        for(int i = 0; i < lines.size(); ++i){
            String tmpLine = lines.get(i).strip();
            tmpLine = tmpLine.replaceAll(" ", "");
            counter ++;
            if(tmpLine.endsWith("{")){
                break;
            }
        }
        for(int i = counter; i < lines.size()-1; ++i){
            retList.add(lines.get(i));
        }
        String ret = String.join("\n", retList);
        return ret;
    }

    public void validPatchExpressAPR(List<Triple<String, Double, String>>patchFunc, String srcFile, String binFile, String buggyMethodFile){
        int startLine = _subject.getFuncBeginLine();
        int endLine = _subject.getFuncEndLine();

        List<Double> scores = new ArrayList<>();
        List<String> patches = new ArrayList<>();
        List<String> traces = new ArrayList<>();
        for(Triple<String, Double, String> patch: patchFunc){
            patches.add(patch.getFirst());
            scores.add(patch.getSecond());
            traces.add(patch.getThird());
        }

        List<Integer> distribution = new ArrayList<>(10);
        for(int i = 0; i < 10; ++i){
            distribution.add(0);
        }
        for(Double score: scores){
            int index = score/0.1 < 10 ? (int)(score/0.1) : 9;
            distribution.set(index, distribution.get(index)+1);
        }

        List<Triple<String, Double, String>> rankedPatches = null;
        rankedPatches = rankPatches1(patchFunc, distribution.get(0));

        scores = new ArrayList<>();
        patches = new ArrayList<>();
        traces = new ArrayList<>();
        for(Triple<String, Double,String> patch: rankedPatches){
            patches.add(patch.getFirst());
            scores.add(patch.getSecond());
            traces.add(patch.getThird());
        }
        JavaFile.writeStringToFile(_logfile, "Validating "+patches.size()+" patches...\n", true);

        List<String> sources = JavaFile.readFileToStringList(srcFile);

        List<String> preList     = new ArrayList<>(sources.subList(0, startLine-1));
        List<String> afterList   = new ArrayList<>(sources.subList(endLine-1, sources.size()));
        List<String> oricontentList = new ArrayList<>(sources.subList(startLine-1, endLine-1));

        List<String> preprocessRes = preprocessFunction(preList, oricontentList);
        String pre = preprocessRes.get(0);
        String oricontent = preprocessRes.get(1);
        String after = String.join("\n", afterList);

        String jsonlPath = "/data/PLM4APR/tmp/exapr/"+_subject.getName()+"/"+_subject.getName()+"_"+_subject.getId()+"_buggy/result.jsonl";
        if(new File(jsonlPath).exists()){
            ExecuteCommand.execute(CmdFactory.createCommand("rm "+ jsonlPath), _subject.getJDKHome());
        }
        String earlyPatchPath = "/data/PLM4APR/GenPat/patches/result0.json";
        if(new File(earlyPatchPath).exists()){
            ExecuteCommand.execute(CmdFactory.createCommand("rm /data/PLM4APR/GenPat/patches/result*.json"), _subject.getJDKHome());
        }
//        StringBuilder builder = new StringBuilder();
//        builder.append("{\n")
//                .append(" \"manifest_version\": 3,").append("\n")
//                .append(" \"interface\": \"defects4j\",").append("\n")
//                        .append(" \"bug\": ").append("\"").append(_subject.getName().substring(0,1).toUpperCase()).append(_subject.getName().substring(1)).append("-").append(_subject.getId()).append("\",\n")
//                        .append(" \"filename\": ").append("\"").append(Utils.join(Constant.SEP, _subject.getSsrc(), _subject.getJavaPath())).append("\",\n")
//                        .append(" \"contex_above\": ").append("\"").append(String.join("\n", pre)).append("\",\n")
//                        .append(" \"contex_below\": ").append("\"").append(String.join("\n", after)).append("\",\n")
//                        .append(" \"unpatched\": ").append("\"").append(String.join("\n", oricontent)).append("\",\n")
//                        .append(" \"patches\": [\n");
        Map<String, Object> jsonDS = new LinkedHashMap<>();
        jsonDS.put("manifest_version", 3);
        jsonDS.put("interface", "defects4j");
        switch (_subject.getName()) {
            case "jacksoncore":
                jsonDS.put("bug", "JacksonCore-" + _subject.getId());
                break;
            case "jacksondatabind":
                jsonDS.put("bug", "JacksonDatabind-" + _subject.getId());
                break;
            case "jacksonxml":
                jsonDS.put("bug", "JacksonXml-" + _subject.getId());
                break;
            case "jxpath":
                jsonDS.put("bug", "JxPath-" + _subject.getId());
                break;
            default:
                jsonDS.put("bug", _subject.getName().substring(0, 1).toUpperCase() + _subject.getName().substring(1) + "-" + _subject.getId());
                break;
        }
        jsonDS.put("filename", Utils.join(Constant.SEP, _subject.getSsrc(), _subject.getJavaPath()).substring(1));
        jsonDS.put("context_above", pre);
        jsonDS.put("unpatched", oricontent);
        jsonDS.put("context_below", after);
        for(int slice = 0; slice < 5; ++ slice){
            int begin = slice * 250;
            if(begin > patches.size()) break;
            int end = Math.min(patches.size(), (slice+1) * 250);
            List<String> tmpPatches = new ArrayList<>(patches.subList(begin, end));
            List<String> tmpTraces  = new ArrayList<>(traces.subList(begin, end));
            for(String patch: tmpPatches){
                List<String> tmp = new ArrayList<>();
                String tmppatch = preprocessFunction(patch);
                tmp.add(tmppatch);
                jsonDS.put("patches", tmp);
                Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
                String json = gson.toJson(jsonDS);
                JavaFile.writeStringToFile("/data/PLM4APR/GenPat/patches/result"+tmpPatches.indexOf(patch)+".json", json, false);
            }
            LevelLogger.info("Validating patches...");
            ExecuteCommand.execute(CmdFactory.createExpressAPRCommand(_subject), _subject.getJDKHome());
            LevelLogger.info("Validating patches finished!");
            _validPatchRank += tmpPatches.size();
            Gson gson = new Gson();
            List<String> jsonl = JavaFile.readFileToStringList(jsonlPath);
            for(String json: jsonl){
                Map<String, Object> tmp = gson.fromJson(json, Map.class);
                String[] path_info = tmp.get("patches_path").toString().split("/");
                String id = path_info[path_info.length-1].split("\\.")[0].split("result")[1];
                String patch = tmpPatches.get(Integer.parseInt(id));
                String trace = tmpTraces.get(Integer.parseInt(id));
                boolean fixed = tmp.get("succlist") != null && tmp.get("succlist").equals("s");
                _allTestedPatches += 1;
                if(tmp.get("succlist") != null && tmp.get("succlist").equals("C")){
                    _compileFailedPatches += 1;
                }
                if(fixed){
                    if(_generatedPatches.contains(patch)) {
                        continue;
                    }
                    _generatedPatches.add(patch);
                    _patchNum += 1;
                    writeLog(patch, srcFile, fixed, _patchNum, trace);
                    String diff = gitDiff.validFixes(patch, buggyMethodFile);
                    diff = diff.replaceAll(" ", "");
                    JavaFile.writeStringToFile("./repair-result/patch/"+_subject.getName()+"/"+_subject.getId()+".diff", "Following diff " + _currentPatchId + ":\n", true);
                    JavaFile.writeStringToFile("./repair-result/patch/"+_subject.getName()+"/"+_subject.getId()+".diff", diff, true);
                    LevelLogger.info("Correct fixed!!! Patch Rank: " + id +"/"+_validPatchRank);
//                    break;
                }
            }
            ExecuteCommand.execute(CmdFactory.createCommand("rm "+ jsonlPath), _subject.getJDKHome());
            if(shouldStop()){
                break;
            }
        }

    }

    public boolean validPatch(List<Triple<String, Double, String>> patchFunc, String buggyMethodFile, String patchMethodFile, String srcFile, String binFile){
        int startLine = _subject.getFuncBeginLine();
        int endLine   = _subject.getFuncEndLine();

        String oriSource     = JavaFile.readFileToString(srcFile);

        List<Double> scores = new ArrayList<>();
        List<String> patches = new ArrayList<>();
        List<String> traces = new ArrayList<>();
        for(Triple<String, Double, String> patch: patchFunc){
            patches.add(patch.getFirst());
            scores.add(patch.getSecond());
            traces.add(patch.getThird());
        }

        List<Integer> distribution = new ArrayList<>(10);
        for(int i = 0; i < 10; ++i){
            distribution.add(0);
        }
        for(Double score: scores){
            int index = score/0.1 < 10 ? (int)(score/0.1) : 9;
            distribution.set(index, distribution.get(index)+1);
        }

        List<Triple<String, Double, String>> rankedPatches = null;
        rankedPatches = rankPatches1(patchFunc, distribution.get(0));

        scores = new ArrayList<>();
        patches = new ArrayList<>();
        traces = new ArrayList<>();
        for(Triple<String, Double,String> patch: rankedPatches){
            patches.add(patch.getFirst());
            scores.add(patch.getSecond());
            traces.add(patch.getThird());
        }

        String groundTruth   = JavaFile.readFileToString("./resources/groundTruth/"+_subject.getLLmName()+"/"+_subject.getName()+"_"+_subject.getId()+".txt");
        List<String> sources = JavaFile.readFileToStringList(srcFile);

        List<String> pre     = sources.subList(0, startLine-1);
        List<String> after   = sources.subList(endLine, sources.size());


        int counter = 0;
        // filter out not compilable patches
//        List<String> adaptedPatches = new ArrayList<>();
//        for(String patch: patches){
//            counter ++;
//            String newFileStr = String.join("\n", pre) + patch + String.join("\n", after);
//            if(counter == 1) JavaFile.writeStringToFile(_logfile, newFileStr, true);
//            if (compileValid(srcFile, newFileStr) == ValidateResult.PASS) {
//                adaptedPatches.add(patch);
//                printProgress(patches.indexOf(patch), patches.size());
//            } else {
//                LevelLogger.info(counter + "/" + patchFunc.size() + " Patch failed to compile!");
//            }
//        }
//        LevelLogger.info("Passed files rate:"+adaptedPatches.size()+"/"+rankedPatches.size());
//        JavaFile.writeStringToFile(_logfile, "Passed files rate:"+adaptedPatches.size()+"/"+rankedPatches.size()+"\n", true);
//        LevelLogger.info("Validating patches...");
//        counter = 0;
        for(String patch: patches){
            _validPatchRank ++;
            counter ++;
            printProgress(counter, patches.size());
            JavaFile.writeStringToFile(_logfile, scores.get(patches.indexOf(patch))+"\n"+patch, true);
//            JavaFile.writeStringToFile(_logfile, patch, true);
            // test by execute testcases
//            String newFileStr = String.join("\n", pre) + patch + String.join("\n", after);
//            JavaFile.writeStringToFile(srcFile, newFileStr);
//            Utils.deleteFiles(binFile);
//            boolean passtest = testValid() == ValidateResult.PASS;

            // diff based test
            String diff = gitDiff.validFixes(patch, buggyMethodFile);
            diff = diff.replaceAll(" ", "");
            JavaFile.writeStringToFile(_logfile, diff+"\n", true);
            boolean pass = diff.equals(groundTruth);
            _patchNum += pass ? 1 : 0;
            if(pass) {
                writeLog(patch, srcFile, pass, counter);
                LevelLogger.info("Correct fixed!!! Patch Rank: " + counter +"/"+_validPatchRank);
                break;
            }
        }
        JavaFile.writeStringToFile(_logfile, distribution+"\n", true);
        _subject.restore();
        return _patchNum > 0;
    }

    private Node getMethodNode(String file){
        final CompilationUnit unit = JavaFile.genASTFromFileWithType(file);
        final List<MethodDeclaration> lst = new ArrayList<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration node) {
                lst.add(node);
                return false;
            }
        });
        if (lst.isEmpty()) return null;
        StmtParser parser = new StmtParser();
        return parser.setCompilationUnit(file, unit).process(lst.get(0));
    }

//    public void statisticOfPathes() throws IOException {
//        final String srcSrc = _subject.getHome() + _subject.getSsrc();
//        final String srcBin = _subject.getHome() + _subject.getSbin();
//        final String srcFile= Utils.join(Constant.SEP, srcSrc, _subject.getJavaPath());
//        final String binFile= Utils.join(Constant.SEP, srcBin, _subject.getJavaPath().substring(0, _subject.getJavaPath().length()-4)+"class");
//        String buggyMethodFile = "/data/PLM4APR/codex_out/potential_bugs_starcoder_real/"+_subject.getName()+"_"+_subject.getId()+"/buggy.java";
//        String patchesDir = "/data/PLM4APR/codex_out/200_patches_starcoder/"+_subject.getName()+"_"+_subject.getId();
//        List<String> patchFuncs = new ArrayList<>();
//        JavaFile.ergodic(patchesDir, patchFuncs);
//        Node bNode = getMethodNode(buggyMethodFile);
//        JavaFile.writeStringToFile(_logfile, "Start fixing bug:"+ _subject.getName()+"_"+_subject.getId() +"\n", false);
//        for(String patchFunc: patchFuncs){
//            System.out.println("current patch:"+patchFunc);
////            System.in.read();
//            Node pNode = getMethodNode(patchFunc);
//            if (bNode == null || pNode == null) {
//                String err = "Get MethodDecl node failed!#" + patchFunc;
//                LevelLogger.error(err);
//                JavaFile.writeStringToFile(_logfile, err + "\n", true);
//                continue;
//            }
//            gitDiff diff = new gitDiff(buggyMethodFile, patchFunc);
//            Map<Integer, Integer> keepMap = new HashMap<>();
//            // patch keep lines To bug keep lines
//            Map<Integer, Integer> keepMapRev = new HashMap<>();
//            for(int i =0; i<diff.getPKeeps().size();++i){
//                keepMap.put(diff.getBKeeps().get(i), diff.getPKeeps().get(i));
//                keepMapRev.put(diff.getPKeeps().get(i), diff.getBKeeps().get(i));
//            }
////            System.out.println("KEEPS:"+keepMap);
//            ExecutorService service = Executors.newSingleThreadExecutor();
//            StaticCheck checker = new StaticCheck(srcFile, buggyMethodFile, null, _subject);
//            NodeMatcher matcher = new NodeMatcher(bNode, pNode, diff.getDeletions(), diff.getAdds(), diff.getAddBracket(), diff.getDeleteBracket(), keepMapRev, 60, checker);
//            Future<List<List<MyActions>>> task = service.submit(matcher);
//            List<List<MyActions>> fixPositions = null;
//            try {
//                fixPositions = task.get(600, TimeUnit.MINUTES);
//            } catch (Exception e) {
//                LevelLogger.debug("Repair match failed!");
//                task.cancel(true);
//                LevelLogger.debug("Cancel task now!");
//            }
//            JavaFile.writeStringToFile(_logfile, "Following are "+ patchFuncs.indexOf(patchFunc) +"th generated actions:\n", true);
//            if(fixPositions== null || fixPositions.isEmpty()){
//                JavaFile.writeStringToFile(_logfile, patchFunc+":NAN\n", true);
//            }else{
//                List<MyActions> actions = fixPositions.get(fixPositions.size()-1);
//                StringBuilder builder = new StringBuilder();
//                for(MyActions action: actions){
//                   if(action instanceof MyDeletion){
//                       MyDeletion tmp = (MyDeletion) action;
//                       builder.append(" DELETE:"+ tmp.getCurNode().getNodeType().toString()+";"+(tmp.getEndLine()-tmp.getStartLine()+1)).append("\n");
//                   }else if(action instanceof MyInsertion){
//                       MyInsertion tmp = (MyInsertion) action;
//                       builder.append(" INSERT:"+ tmp.getCurNode().getNodeType().toString()+";"+(tmp.getCurNode().getEndLine()-tmp.getCurNode().getStartLine()+1)).append("\n");
//                   }else if(action instanceof MyUpdate){
//                       MyUpdate tmp = (MyUpdate) action;
//                       builder.append(" UPDATE:"+ tmp.getCurNode().getNodeType().toString()+";"+(tmp.getCurNode().getEndLine()-tmp.getCurNode().getStartLine()+1)).append("\n");
//                   }else if(action instanceof MyMove){
//                       MyMove tmp = (MyMove) action;
//                       builder.append(" MOVE:"+ tmp.getCurNode().getNodeType().toString()+";"+(tmp.getCurNode().getEndLine()-tmp.getCurNode().getStartLine()+1)).append("\n");
//                   }else if(action instanceof Wrap){
//                          Wrap tmp = (Wrap) action;
//                          builder.append("  WRAP:"+ tmp.getCurNode().getNodeType().toString()+";"+(tmp.getCurNode().getEndLine()-tmp.getCurNode().getStartLine()+1)).append("\n");
//                   }else{
//                       System.err.println("ERROR NODE TYPE!!!");
//                       System.exit(-1);
//                   }
//                }
//                String outString = builder.toString();
//                JavaFile.writeStringToFile(_logfile, outString, true);
//            }
//        }
//        LevelLogger.debug("Generating done!");
//    }

    public void repair1(){
        final String srcSrc = _subject.getHome() + _subject.getSsrc();
        final String srcBin = _subject.getHome() + _subject.getSbin();
        final String srcFile= Utils.join(Constant.SEP, srcSrc, _subject.getJavaPath());
        final String binFile= Utils.join(Constant.SEP, srcBin, _subject.getJavaPath().substring(0, _subject.getJavaPath().length()-4)+"class");

//        String buggyMethodFile = "/Users/ffengjay/Postgraduate/PLM4APR/codex_out/potential_bugs_starcoder/"+_subject.getName()+"_"+_subject.getId()+"/buggy.java";
//        String patchesDir      = "/Users/ffengjay/Postgraduate/PLM4APR/codex_out/valuable_patches_starcoder/"+_subject.getName()+"_"+_subject.getId();
        // for Mac
//        String buggyMethodFile = "/Users/ffengjay/Postgraduate/PLM4APR/codex_out/potential_bugs_gpt35_real/"+_subject.getName()+"_"+_subject.getId()+"/buggy.java";
//        String patchesDir      = "/Users/ffengjay/Postgraduate/PLM4APR/codex_out/valuable_patches_gpt35_real/"+_subject.getName()+"_"+_subject.getId();
        // for Linux
        String buggyMethodFile = "/data/PLM4APR/codex_out/potential_bugs_"+_subject.getLLmName()+"_real/"+_subject.getName()+"_"+_subject.getId()+"/buggy.java";
        String patchesDir      = "/data/PLM4APR/codex_out/valuable_patches_"+_subject.getLLmName()+"_real/"+_subject.getName()+"_"+_subject.getId();
//        String patchesDir      = "/data/PLM4APR/codex_out/200_patches_"+_subject.getLLmName()+"/"+_subject.getName()+"_"+_subject.getId();
        List<String> patchFuncs = new ArrayList<>();
        JavaFile.ergodic(patchesDir, patchFuncs);
//        LevelLogger.debug("patchFuncs:"+patchFuncs);
//        for(int patchid =0; patchid < patchFuncs.size(); ++patchid) {
//            if(patchid > 59){
//                break;
//            }
//            _allTestedPatches = 0;
//            _compileFailedPatches = 0;
//            _currentPatchId = patchid;
//            String patchFunc = patchesDir+"/"+patchid+".java";
        for(String patchFunc: patchFuncs){
            JavaFile.writeStringToFile(_logfile, "Current file:"+patchFunc+"\n", true);
            _validPatchRank = 0;
//            if(patchid != 36)
//                continue;
            if (shouldStop()) {
                break;
            }
            _alreadyGenerated.clear();

            String message = "Patch Number: " + patchFunc;
            LevelLogger.info(message);
            JavaFile.writeStringToFile(_logfile, message + "\n", true);

            _subject.restore(srcSrc);// restore original source code
            Node bNode = getMethodNode(buggyMethodFile);
            Node pNode = getMethodNode(patchFunc);
            if (bNode == null || pNode == null) {
                String err = "Get MethodDecl node failed!#" + patchFunc;
                LevelLogger.error(err);
                JavaFile.writeStringToFile(_logfile, err + "\n", true);
                continue;
            }
            gitDiff diff = new gitDiff(buggyMethodFile, patchFunc);
//            diff.extractDiff();
            System.out.println(diff);
            System.out.println("ADDS:"+diff.getAdds());
            System.out.println("DELETIONS:"+diff.getDeletions());
            System.out.println("DELETION BRACKET:"+diff.getDeleteBracket());
            // bug keep lines To patch keep lines
            Map<Integer, Integer> keepMap = new HashMap<>();
            // patch keep lines To bug keep lines
            Map<Integer, Integer> keepMapRev = new HashMap<>();
            for(int i =0; i<diff.getPKeeps().size();++i){
                keepMap.put(diff.getBKeeps().get(i), diff.getPKeeps().get(i));
                keepMapRev.put(diff.getPKeeps().get(i), diff.getBKeeps().get(i));
            }
            System.out.println("KEEPS:"+keepMap);
            ExecutorService service = Executors.newSingleThreadExecutor();
            StaticCheck checker = new StaticCheck(srcFile, patchFunc, null, _subject);
            NodeMatcher matcher = new NodeMatcher(bNode, pNode, diff.getDeletions(), diff.getAdds(), diff.getAddBracket(), diff.getDeleteBracket(), keepMapRev, 60, checker, _subject.getLogFile());
            Future<List<List<List<MyActions>>>> task = service.submit(matcher);
            List<List<List<MyActions>>> fixPositions = null;
            try {
                fixPositions = task.get(600, TimeUnit.MINUTES);
            } catch (Exception e) {
                LevelLogger.debug("Repair match failed!");
                task.cancel(true);
                LevelLogger.debug("Cancel task now!");
            }
            LevelLogger.debug("Try to shut down server.");
            service.shutdownNow();
            LevelLogger.debug("Finish shutting down server.");
//            continue;
            Apply applyer = new Apply(null, buggyMethodFile, srcFile, _subject.getFuncBeginLine(), _subject.getFuncEndLine(), keepMap, diff.getAppendAbracket(), diff.getDeleteBracket());
            // apply all changes
//            LevelLogger.debug("Example2:"+fixPositions.get(fixPositions.size()-1));
            assert fixPositions != null;
            for(List<List<MyActions>> fixPosition: fixPositions){
                List<String> patches = new ArrayList<>();
                List<Pair<String, Double>> patchesV2 = new ArrayList<>();
                List<Triple<String, Double, String>> patchesV3 = new ArrayList<>();
                // for each selected fix actions list
                if(fixPositions.indexOf(fixPosition) > 4){
                    break;
                }
                JavaFile.writeStringToFile(_logfile, "Following are "+ fixPositions.indexOf(fixPosition) +"th generated patches:\n", true);
                String tmpTrace = "";
                for(List<MyActions> actions: fixPosition){
                    try {
                        applyer.setActions(actions);
                        applyer.applyOnFuncByLine();
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                    String patch = applyer.getPatchFunc();
                    Double similarity = 0.0;
                    for(MyActions actionsCandidate: actions){
                        similarity += actionsCandidate.getEditDistance();
                    }
                    for(MyActions actionsCandidate: actions){
                        if(actionsCandidate.getTrace().isEmpty()){
                            continue;
                        }
                        tmpTrace = String.join(";",actionsCandidate.getTrace());
                    }
                    patches.add(patch);
                    patchesV2.add(new Pair<>(patch, similarity));
                    patchesV3.add(new Triple<>(patch, similarity, tmpTrace));
                }
//                boolean fixed = validPatch(patchesV3, buggyMethodFile, patchFunc,srcFile, binFile);
//                if(fixed){
//                    LevelLogger.info("FIXED!!!");
//                    LevelLogger.info("Patch found in "+fixPositions.indexOf(fixPosition)+" th fixpositions!");
//                    break;
//                }
                validPatchExpressAPR(patchesV3, srcFile, binFile, buggyMethodFile);
                if (shouldStop()) {
                    break;
                }
            }
            JavaFile.writeStringToFile(_logfile, "Tested:" + _allTestedPatches + " patches\n", true);
            JavaFile.writeStringToFile(_logfile, "Compile failed:" + _compileFailedPatches + " patches\n", true);
            double compilationRate = (_allTestedPatches - _compileFailedPatches)*1.0/_allTestedPatches;
            JavaFile.writeStringToFile(_logfile, "Compilation rate:" + compilationRate + "\n", true);
        }
    }

    public void repair() {
        String message = "Repair : " + _subject.getName() + "_" + _subject.getId();
        JavaFile.writeStringToFile(_logfile, message + "\n", false);
        LevelLogger.info(message);
        _subject.backup(); // back up all test source files and proj source files

        String srcSrc = _subject.getHome() + _subject.getSsrc();
        String testSrc = _subject.getHome() + _subject.getTsrc();
        String srcBin = _subject.getHome() + _subject.getSbin();
        String testBin = _subject.getHome() + _subject.getTbin();

        Utils.deleteDirs(srcBin, testBin);

        // split failed test case into several cases and write to original test file.
        // but, seems not necessary...
//        Purification purification = new Purification(_subject);
//        List<String> purifiedFailedTestCases = purification.purify(_subject.purify());
//        if(purifiedFailedTestCases == null || purifiedFailedTestCases.isEmpty()){
//            purifiedFailedTestCases = purification.getFailedTest();
//        }
//        _allFailedTests.addAll(purifiedFailedTestCases);
//        _subject.backupPurifiedTest(); // backup the purified test cases

//        Purification purification = new Purification(_subject);
//        _allFailedTests = new HashSet<>(purification.getFailedTest());


        String start = _timer.start();
        LevelLogger.info(start);
        int all = 0;

        _patchNum = 0;
        _subject.restore(srcSrc);
        Utils.deleteDirs(srcBin, testBin);

        repair1();
        all += _patchNum;

        _subject.restore(); //restore all proj and test source files
        _patchNum = all;
        message = "Finish : " + _subject.getName() + "-" + _subject.getId() + " > patch : " + all
                + " | Start : " + start + " | End : " + simpleDateFormat.format(new Date());
        JavaFile.writeStringToFile(_logfile, message + "\n", true);
        LevelLogger.info(message);
    }

    public void extractDiff(){

        String buggyMethodFile = "/data/PLM4APR/codex_out/buggy_methods/" + _subject.getName() + "-" + _subject.getId() + ".java";
        List<String> all_patches_dir = new ArrayList<>();
        List<String> ALL_LLM_NAMES = Arrays.asList("gpt35", "starcoder", "codellama", "llama");
//        List<String> ALL_LLM_NAMES = Arrays.asList("codellama");
        for(String llmName: ALL_LLM_NAMES){
            String patchesDir = "/data/PLM4APR/codex_out/200_patches_" + llmName + "_all_ori/" + _subject.getName().toLowerCase() + "_" + _subject.getId();
            if (!new File(patchesDir).exists()) {
                LevelLogger.info(patchesDir + " not exists!");
                continue;
            }
            List<String> patchFuncs = new ArrayList<>();
            JavaFile.ergodic(patchesDir, patchFuncs);
            List<String> already = new ArrayList<>();
            for(int i =0; i<patchFuncs.size(); ++i){
                String patchFuncPath = patchesDir + "/" + i + ".java";
                String content = JavaFile.readFileToString(patchFuncPath);
                if (already.contains(content)){
                    continue;
                }else {
                    already.add(content);
                    all_patches_dir.add(patchFuncPath);
                }
            }
        }
        for(String patchFunc: all_patches_dir){
            System.out.println("current patch:"+patchFunc + ": " + all_patches_dir.indexOf(patchFunc) + "/" + all_patches_dir.size());
            Node bNode = getMethodNode(buggyMethodFile);
            Node pNode = getMethodNode(patchFunc);
            if (bNode == null || pNode == null) {
                String err = "Get MethodDecl node failed!#" + patchFunc;
                LevelLogger.error(err);
                continue;
            }
            gitDiff diff = new gitDiff(buggyMethodFile, patchFunc);
            diff.extractDiff();
            Map<Integer, Integer> keepMap = new HashMap<>();
            Map<Integer, Integer> keepMapRev = new HashMap<>();
            for(int i =0; i<diff.getPKeeps().size();++i){
                keepMap.put(diff.getBKeeps().get(i), diff.getPKeeps().get(i));
                keepMapRev.put(diff.getPKeeps().get(i), diff.getBKeeps().get(i));
            }
            ExecutorService service = Executors.newSingleThreadExecutor();
            NodeMatcher matcher = new NodeMatcher(bNode, pNode, diff.getDeletions(), diff.getAdds(), diff.getAddBracket(), diff.getDeleteBracket(), keepMapRev, 60, null, _subject.getLogFile());
            Future<List<List<List<MyActions>>>> task = service.submit(matcher);
            List<List<List<MyActions>>> fixPositions = null;
            try {
                fixPositions = task.get(5, TimeUnit.MINUTES);
            } catch (Exception e) {
                LevelLogger.debug("Repair match failed!");
                task.cancel(true);
                LevelLogger.debug("Cancel task now!");
                continue;
            }
            List<MyActions> diffActions = fixPositions.get(0).get(0);
            JavaFile.writeStringToFile("./actions.txt", patchFunc + "\n", true);
            for(MyActions action: diffActions){
                JavaFile.writeStringToFile("./actions.txt", String.valueOf(diffActions.indexOf(action))+":", true);
                JavaFile.writeStringToFile("./actions.txt", action.getType() + ":" + action.getCurNode().getClass().getName()+"\n", true);
            }
        }
        return;
    }

    private final static String COMMAND = "<command> (-d4j <arg>) " +
            "(-d4jhome <arg>)";

    private static Options options() {
        Options options = new Options();

        Option option = new Option("d4j", "d4jBug", true, "Bug id in defects4j, e.g. chart_1");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("d4jhome", "defects4jHome", true, "Home directory of defects4j buggy code. e.g. /path/to/defects4j/checkout/workingDir/");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("modelname", "modelname", true, "Name of the language model. e.g. gpt35");
        option.setRequired(true);
        options.addOption(option);
        return options;
    }

    private static Triple<String, Set<String>, List<Subject>> parseCommandLine(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp(COMMAND, options);
            System.exit(1);
        }

        String singlePattern = null;
        Set<String> patternFile = null;
        List<Subject> subjects = new LinkedList<>();
        if (cmd.hasOption("d4j")) {
            String[] ids = cmd.getOptionValue("d4j").split(",");
            String base = cmd.hasOption("d4jhome") ? cmd.getOptionValue("d4jhome") : Constant.D4J_PROJ_DEFAULT_HOME;
            String llmName = cmd.hasOption("modelname")? cmd.getOptionValue("modelname") : "gpt35";
            boolean memCompile = true;
            // math_1,lang_1-10,
            for (String id : ids) {
                String[] info = id.split("_");
                if (info.length != 2) {
                    LevelLogger.error("Input format error : " + id);
                    continue;
                }
                String name = info[0];
                String[] seqs = info[1].split("-");
                D4jSubject subject;
                if (seqs.length == 1) {
                    int number = Integer.parseInt(seqs[0]);
                    subject = new D4jSubject(base, name, number, memCompile);
                    subject.setLlmName(llmName);
                    subjects.add(subject);
                } else if (seqs.length == 2) {
                    int start = Integer.parseInt(seqs[0]);
                    int end = Integer.parseInt(seqs[1]);
                    for (; start <= end; start ++) {
                        subject = new D4jSubject(base, name, start, memCompile);
                        subjects.add(subject);
                    }
                } else {
                    LevelLogger.error("Input format error : " + id);
                }
            }
        }
        else {
            return null;
        }
        return new Triple<>(singlePattern, patternFile, subjects);
    }

    public static void repairAPI(String[] args) {
        Triple<String, Set<String>, List<Subject>> pair = parseCommandLine(args);
        if (pair == null) {
            LevelLogger.error("Wrong command line input!");
            return;
        }

        String singlePattern = pair.getFirst();
        Set<String> patternRecords = pair.getSecond();
        List<Subject> subjects = pair.getThird();
        String file = Utils.join(Constant.SEP, Constant.HOME, "repair.rec");
        for (Subject subject : subjects) {
            JavaFile.writeStringToFile(file, subject.getName() + "_" + subject.getId() + " > PATCH : ", true);
            LevelLogger.info(subject.getHome() + ", " + subject.toString());
            Repair repair = new Repair(subject, patternRecords, singlePattern);
            repair.repair();
            JavaFile.writeStringToFile(file, repair.patch() + "\n", true);
        }
    }

//    public static void statisticAPI(String[] args) throws IOException {
//        Triple<String, Set<String>, List<Subject>> pair = parseCommandLine(args);
//        if (pair == null) {
//            LevelLogger.error("Wrong command line input!");
//            return;
//        }
//
//        String singlePattern = pair.getFirst();
//        Set<String> patternRecords = pair.getSecond();
//        List<Subject> subjects = pair.getThird();
//        String file = Utils.join(Constant.SEP, Constant.HOME, "repair.rec");
//        for (Subject subject : subjects) {
//            JavaFile.writeStringToFile(file, subject.getName() + "_" + subject.getId() + " > PATCH : ", true);
//            LevelLogger.info(subject.getHome() + ", " + subject.toString());
//            Repair repair = new Repair(subject, patternRecords, singlePattern);
////            repair.statisticOfPathes();
//            JavaFile.writeStringToFile(file, repair.patch() + "\n", true);
//            LevelLogger.debug("For loop done!/"+subjects.size());
//        }
//        LevelLogger.debug("All done!");
//    }
    public static void diffAPI(String[] args){
        Triple<String, Set<String>, List<Subject>> pair = parseCommandLine(args);
        if (pair == null) {
            LevelLogger.error("Wrong command line input!");
            return;
        }
        List<Subject> subjects = pair.getThird();
        String file = Utils.join(Constant.SEP, Constant.HOME, "repair.rec");
        for (Subject subject : subjects) {
            JavaFile.writeStringToFile(file, subject.getName() + "_" + subject.getId() + " > PATCH : ", true);
            LevelLogger.info(subject.getHome() + ", " + subject.toString());
            Repair repair = new Repair(subject, null, null);
            repair.extractDiff();
            JavaFile.writeStringToFile(file, repair.patch() + "\n", true);
        }
        return;
    }

    public static void main(String[] args) {
        Repair.repairAPI(args);
    }
}
