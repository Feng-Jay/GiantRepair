package mfix.core.node.diff;

import mfix.common.cmd.ExecuteCommand;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


public class gitDiff {

    // buggy and patch filePath
    private final String _bFileName;
    private final String _pFileName;

    // deletionLines
    private final List<Integer> _deletions;
    private final List<Integer> _deletionBracket; // delete a "}", special process
    private final List<Integer> _adds;
    private final List<Integer> _addBracket; // add a "}", special process

    // bug and patch file's same lines
    private final List<Integer> _bKeeps;
    private final List<Integer> _pKeeps;

    // diff outcome, show as: +...; -...; ...
    private final List<String> _diffOutcome;

    // counter for buggy and patch file
    private Integer _buggyContentLines;
    private Integer _patchContentLines;
    private Boolean appendABracket = false;

    public gitDiff(String bFileName, String pFileName){
        _bFileName = bFileName;
        _pFileName = pFileName;
        _deletions = new ArrayList<>();
        _deletionBracket = new ArrayList<>();
        _adds      = new ArrayList<>();
        _addBracket= new ArrayList<>();
        _bKeeps    = new ArrayList<>();
        _pKeeps    = new ArrayList<>();
        _diffOutcome = new ArrayList<>();
        _buggyContentLines = 1;
        _patchContentLines = 1;
        extractDiff();
    }
    public List<Integer> getAdds(){return _adds;}
    public List<Integer> getDeletions(){return _deletions;}

    public List<Integer> getDeleteBracket(){return _deletionBracket;}

    public List<Integer> getAddBracket(){return _addBracket;}
    public Boolean getAppendAbracket(){return appendABracket;}

    public List<Integer> getBKeeps(){return _bKeeps;}
    public List<Integer> getPKeeps(){return _pKeeps;}

    public void extractDiff(){

        StringBuilder buggyContent = new StringBuilder();
        try(BufferedReader reader1 = new BufferedReader(new FileReader(_bFileName))){
            String line = reader1.readLine();
            while((line = reader1.readLine()) != null){
                if(reader1.ready()){
                    buggyContent.append(line).append("\n");
                    _buggyContentLines += 1;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        StringBuilder patchContent = new StringBuilder();
        try(BufferedReader reader2 = new BufferedReader(new FileReader(_pFileName))){
            String line = reader2.readLine();
            while((line = reader2.readLine()) != null){
                if(reader2.ready()){
                    patchContent.append(line).append("\n");
                    _patchContentLines += 1;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        // for Mac
//        String tmpBuggy = "/Users/ffengjay/Postgraduate/PLM4APR/tmpBuggy.java";
//        String tmpPatch = "/Users/ffengjay/Postgraduate/PLM4APR/tmpPatch.java";

        // for Linux
        String tmpBuggy = "/data/PLM4APR/tmpBuggy.java";
        String tmpPatch = "/data/PLM4APR/tmpPatch.java";


        try(BufferedWriter writer = new BufferedWriter(new FileWriter(tmpBuggy))){
            writer.write(buggyContent.toString());
        }catch (IOException e){
            e.printStackTrace();
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(tmpPatch))){
            writer.write(patchContent.toString());
        }catch (IOException e){
            e.printStackTrace();
        }

        String cmd = "git diff " + tmpBuggy + " " + tmpPatch;
        List<String> outcome = ExecuteCommand.executeGitDiff(cmd);
        parseDiffOutcome(outcome);
    }
    private void parseDiffOutcome(List<String> outcome){

        int bBeginLine = 1;
        int pBeginLine = 1;
        int bCounter = 0;
        int pCounter = 0;
        boolean beginMatch = false;

        for(String line: outcome){
            if(line.startsWith("@@") ){
                String buggyStart = line.split(" ")[1];
                bBeginLine = -1*Integer.parseInt(buggyStart.split(",")[0])+1;

                String patchStart = line.split(" ")[2];
                pBeginLine = Integer.parseInt(patchStart.split(",")[0])+1;

                bCounter = 0; pCounter = 0;
                beginMatch = true;
                continue;
            }
            if(!beginMatch){
                continue;
            }
            if((!line.startsWith("---")) && line.startsWith("-")){
                _diffOutcome.add(line);
                String tmp = line;
                tmp = tmp.replaceAll("[\\s-]+", "");
                if(tmp.equals("}"))
                    _deletionBracket.add(bBeginLine + bCounter);
                else
                    _deletions.add(bBeginLine + bCounter);
                bCounter++;
            }
            else if((!line.startsWith("+++")) && line.startsWith("+")){
                _diffOutcome.add(line);
                String tmp = line;
                tmp = tmp.replaceAll("[ +\n]+", "");
                if(tmp.equals("}"))
                    _addBracket.add(pBeginLine + pCounter);
                else
                    _adds.add(pBeginLine + pCounter);
                pCounter++;
            }else{
                _bKeeps.add(bBeginLine+bCounter);
                _pKeeps.add(pBeginLine+pCounter);
                _diffOutcome.add(line);
                bCounter++;
                pCounter++;
            }
        }
        _bKeeps.add(1); _pKeeps.add(1);
        if( _addBracket.contains(_patchContentLines) && !_pKeeps.contains(_patchContentLines)){
            appendABracket = true;
        }
        LevelLogger.info("buggy_file_unmodified_lines:" + _bKeeps);
        LevelLogger.info("patch_file_unmodified_lines:" + _pKeeps);
//        System.out.println(_bKeeps);
//        System.out.println(_pKeeps);
    }

    public static String validFixes(String patchContent, String buggyMethodPath){
        // for Mac
//        String tmpBuggy = "/Users/ffengjay/Postgraduate/PLM4APR/tmpBuggy.java";
//        String tmpPatch = "/Users/ffengjay/Postgraduate/PLM4APR/tmpPatch.java";

        // for Linux
        String tmpBuggy = "/data/PLM4APR/tmpBuggy1.java";
        String tmpPatch = "/data/PLM4APR/tmpPatch1.java";

        StringBuilder buggyContentBuilder = new StringBuilder();
        List<String> buggyContentList = JavaFile.readFileToStringList(buggyMethodPath);
        for(int i = 0; i < buggyContentList.size(); ++i){
            if(i == 0 || i == buggyContentList.size()-1){
                continue;
            }else{
                buggyContentBuilder.append(buggyContentList.get(i)).append("\n");
            }
        }
        String buggyContent = buggyContentBuilder.toString();

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(tmpBuggy))){
            writer.write(buggyContent);
        }catch (IOException e){
            e.printStackTrace();
        }

        try(BufferedWriter writer = new BufferedWriter(new FileWriter(tmpPatch))){
            writer.write(patchContent);
        }catch (IOException e){
            e.printStackTrace();
        }

        String cmd = "git diff " + tmpBuggy + " " + tmpPatch;
        List<String> outcome = ExecuteCommand.executeGitDiff(cmd);
        List<String> addLines = new ArrayList<>();
        List<String> delLines = new ArrayList<>();
        for(String line: outcome){
            if((!line.startsWith("+++")) && line.startsWith("+")){
                addLines.add(line.replace(" ", ""));
            }
            else if((!line.startsWith("---")) && line.startsWith("-")){
                delLines.add(line.replace(" ", ""));
            }
        }
        StringBuilder builder = new StringBuilder();
        for(String line: delLines){
            if(line.strip().length() == 1){
                continue;
            }
            builder.append(line.strip()).append("\n");
        }
        for(String line: addLines){
            if(line.startsWith("+//") || line.strip().length() == 1)
                continue;
            builder.append(line.strip()).append("\n");
        }
        return builder.toString();
    }

    @Override
    public String toString(){
        StringBuilder builder = new StringBuilder();
        for(String line: _diffOutcome){
            builder.append(line).append("\n");
        }
        return builder.toString();
    }
}
