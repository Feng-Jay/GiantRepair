package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Apply {
    private List<MyActions> _actions;
    private final String _funcFile; // file that only contains method.
    private String _srcFile;
    private String _patchFunc = null;
    private Integer _funcBeginLine;
    private Integer _funcEndLine;
    private final Map<Integer, Integer> _keeps;
    private Boolean _append = false;
    private List<Integer> _deleteBracketLines = new ArrayList<>();

    List<String> _source;

    public Apply(List<MyActions> actions, String funcFile, String srcFile, Integer beginLine, Integer endLine, Map<Integer, Integer> keeps, Boolean append, List<Integer> deleteBreacketLines){
        _actions = actions;
        _funcFile= funcFile;
        _srcFile = srcFile;
        _funcBeginLine= beginLine;
        _funcEndLine  = endLine;
        _keeps = keeps;
        _append = append;

        _deleteBracketLines = deleteBreacketLines;

        try{
            BufferedReader reader = new BufferedReader(new FileReader(_funcFile));

            String line;
            _source = new ArrayList<>();
            while((line =reader.readLine()) != null){
                _source.add(line+"\n");
            }
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    public void setActions(List<MyActions> actions){_actions = actions;}

    public String getPatchFunc(){return _patchFunc;}

    // Fixed BUG:
    // There is a bug:
    //  unmodified code
    //  + update
    //  + insertion
    //  the insertion's line number will be unmodified code line.
    //  but the update's line number will be the line number after insertion.
    //  if apply both of them, the order is wrong.
    //  ...
    //  POTENTIAL FIXES:
    //  1. insertion's line will add 1.
    //  2. when get insert line number, utilize update nodes.

    public void applyOnFuncByLine() throws IOException{
        StringBuilder builder;
        // Insertion: insert Line and corresponding Nodes
        Map<Integer, List<Node>> insert = new HashMap<>();
        // Update: update Line and corresponding Node
        Map<Integer, MyActions> update = new HashMap<>();
        // Deletion: Deletion node's begin & end lines.
        Map<Integer, Integer> skipRanges = new HashMap<>();
        // Insertion Range: insert range lines
        List<Integer> insertRangeLines = new ArrayList<>();

        // git diff's outcome start from 1-index
        // but JDT's getLineNumber start from 1-index
        // but source code is read from 0-index.
//        LevelLogger.debug("Apply: " + _actions.size() + " actions");
        for (MyActions ac : _actions) {
            if (ac instanceof MyInsertion) {
                Node tmpNode = ac.getCurNode();
                int tmpBeginLine = tmpNode.getStartLine();
                int tmpEndLine = tmpNode.getEndLine();
                for (int i = tmpBeginLine; i <= tmpEndLine; ++i) {
                    insertRangeLines.add(i);
                }
                if (insert.containsKey(((MyInsertion) ac).getLine() - 1)) {
                    insert.get(((MyInsertion) ac).getLine() - 1).add(ac.getCurNode());
                } else {
                    List<Node> tmp = new ArrayList<>();
                    tmp.add(ac.getCurNode());
                    insert.put(((MyInsertion) ac).getLine() - 1, tmp);
                }
            } else if (ac instanceof MyDeletion) {
                skipRanges.put(((MyDeletion) ac).getStartLine() - 1, ((MyDeletion) ac).getEndLine());
            } else if (ac instanceof MyUpdate) {
                update.put(((MyUpdate) ac).getStarLine() - 1, ac);
            } else {
                System.err.println("Illegal Action!!!");
            }
        }
//        LevelLogger.debug("Insert lines are:"+insert);
        builder = new StringBuilder();
        builder.append("//Following are generated patch:\n");
        int counter = 0;
        boolean comment = false;
        for(int i =1; i<_source.size()-1; ++i){
            if(counter == i){
                comment = false;
            }
            if(update.containsKey(i)){
                comment = true;
                MyUpdate tmp = (MyUpdate) update.get(i);
                if(tmp.getExprString() == null)
                    builder.append(update.get(i).getCurNode().toString()).append("\n");
                else
                    builder.append(tmp.getExprString());
                counter = ((MyUpdate)update.get(i)).getEndLine() == i ? i+1 : ((MyUpdate)update.get(i)).getEndLine();
            }
            if(skipRanges.containsKey(i)&&skipRanges.get(i)!=0){
                comment = true;
                counter = skipRanges.get(i) == i ? i+1 : skipRanges.get(i);
            }
            if(!comment && !_deleteBracketLines.contains(i+1) ){
                builder.append(_source.get(i));
//                LevelLogger.debug("Apply ori ["+i+"] :"+source.get(i));
            }
            if(insert.containsKey(i)){
                for(int acID = 0; acID < insert.get(i).size(); ++acID) {
                    builder.append(insert.get(i).get(acID).toString()).append("\n");
                }
            }
        }
        if(_append){
            builder.append("}\n");
        }
        _patchFunc = builder.toString();
//        System.out.println(builder.toString());
    }

//    public void applyOnFunc() throws IOException {
//        BufferedReader reader = new BufferedReader(new FileReader(_funcFile));
//        StringBuilder builder = new StringBuilder();
//        String line;
//        while((line =reader.readLine()) != null){
//            builder.append(line+"\n");
//        }
//        String source = builder.toString();
//
//        Map<Integer, Node> insert = new HashMap<>();
//        Map<Integer, MyActions> update = new HashMap<>();
//        Map<Integer, Integer> skipRanges = new HashMap<>();
//        for(MyActions ac: _actions){
//            if(ac instanceof MyInsertion){
//                insert.put(((MyInsertion) ac).getPos(), ac.getCurNode());
//            }else if(ac instanceof MyDeletion){
//                skipRanges.put(ac.getStartPos(), ac.getEndPos());
//            }else {
//                // update
//                update.put(ac.getStartPos(), ac);
//            }
//        }
//        builder = new StringBuilder();
//        Integer counter = 0;
//        for(int i =0; i<source.length(); ++i){
//            boolean comment = false;
//            if(insert.containsKey(i)){
//                builder.append(source.substring(i, i+1));
//                builder.append(insert.get(i).toString()+"\n");
//                continue;
//            }
//            if(update.containsKey(i)){
//                comment = true;
//                builder.append(update.get(i).getCurNode().toString());
//                counter = update.get(i).getEndPos();
//            }
//            if(skipRanges.containsKey(i)&&skipRanges.get(i)!=0){
//                comment = true;
//                counter = skipRanges.get(i);
//            }
//            if(counter == i){
//                comment = false;
//            }
//            if(!comment){
//                builder.append(source.substring(i, i+1));
//            }
//        }
//        System.out.println(builder.toString());
//
//    }

}
