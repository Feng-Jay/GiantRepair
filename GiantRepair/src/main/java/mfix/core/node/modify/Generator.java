package mfix.core.node.modify;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.expr.VarDeclarationExpr;
import mfix.core.node.ast.stmt.*;
import mfix.core.node.check.StaticCheck;

import java.util.*;

/**
 * Modification generator:
 * Given: matching results and all stmts
 * Generate: INSERT DELETE UPDATE & MOVE actions
 */
public class Generator {
    private final List<Node> _bAllStmts;
    private final List<Node> _pAllStmts;

    private final List<Node> _bModified;
    private final List<Node> _pModified;

    private final List<Node> _bMatched;
    private final List<Node> _pMatched;

    private final Map<Integer, Integer> _keepMap;

    private final List<MyActions> _actions;
    private final List<Node> _insertNodes;

    private final Map<Node, Node> _updateNodes;

    private final List<MyActions> _insertions;
    private final List<MyActions> _deltions;
    private final List<MyActions> _updates;
    private final List<MyActions> _moves;
    private List<MyActions> _wraps;


    private final List<Integer> _addBracketLines;

    private final List<Integer> _deleteBracketLines;

    private final StaticCheck _checker;

    private List<List<Integer>> _alreadyNormal;
    private List<List<Integer>> _alreadyFiltered;

    private Map<Node, List<Node>> _node2DelNodes;

    public Generator(List<Node> bAllStmts, List<Node> pAllStmts, List<Node> bMatched, List<Node> pMatched, List<Node> bModified, List<Node> pModified, Map<Integer, Integer> keep, List<Integer> addBracket, List<Integer> deleteBracket, StaticCheck checker){
        // require resources
        _bAllStmts = bAllStmts;
        _pAllStmts = pAllStmts;
        _bMatched  = bMatched;
        _pMatched  = pMatched;
        _bModified = bModified;
        _pModified = pModified;
        _keepMap = keep;
        _addBracketLines = addBracket;
        _deleteBracketLines = deleteBracket;
        // actions generated
        _actions = new ArrayList<>();
        _insertNodes  = new ArrayList<>();
        _updateNodes  = new HashMap<>();
        _insertions = new ArrayList<>();
        _deltions   = new ArrayList<>();
        _updates    = new ArrayList<>();
        _moves      = new ArrayList<>();
        _wraps      = new ArrayList<>();
        _node2DelNodes = new HashMap<>();

        _checker = checker;

        _alreadyNormal = new ArrayList<>();
        _alreadyFiltered = new ArrayList<>();
    }
    public List<MyActions> getActions(){return _actions;}

//    public Map<Integer,Integer> getInsertBracketMap(){return _insertBracketMap;}

    /**
     * Get Insertion Action's insert line. By utilizing the keepMap of two codes
     * Then these actions can be sort by their positions in patch file to apply
     * @param line: insert content's line in patch file
     * @return: insert line in buggy code
     */
    public Integer getInsertLineNew(Integer line){
        Map<Integer, Integer> new2OldLines = new HashMap<>(_keepMap);
        for(MyActions action: _updates){
            MyUpdate update = (MyUpdate) action;
            new2OldLines.put(update.getCurNode().getStartLine(), update.getFormerNode().getStartLine());
        }
        List<Integer> allLines = new ArrayList<>(new2OldLines.keySet());
        allLines.add(line);
        Collections.sort(allLines);
        int index = allLines.indexOf(line);
        int ret;
        if (index == 0){
            for(Node tmp : _bAllStmts){
                if(tmp instanceof MethDecl){
                    ret = tmp.getStartLine();
                    return ret;
                }
            }
            System.err.println("Can not find insert place!!!");
        }
        int insertLine = allLines.get(index-1);
        if(new2OldLines.containsKey(insertLine)){
            return new2OldLines.get(insertLine);
        }
        System.err.println("Can not find insert place!!!");
        return -1;
    }

    /**
     * When given a node, find its insert line.
     * @param node: node to be inserted
     * @return: insert line in buggy code
     */
    public Integer getInsertLineNew(Node node){
        Map<Integer, Integer> new2OldLines = new HashMap<>(_keepMap);
        for(MyActions action: _updates){
            MyUpdate update = (MyUpdate) action;
            new2OldLines.put(update.getCurNode().getStartLine(), update.getFormerNode().getStartLine());
        }
        List<Integer> allLines = new ArrayList<>(new2OldLines.keySet());
        allLines.add(node.getStartLine());
        Collections.sort(allLines);
        int index = allLines.indexOf(node.getStartLine());
        int ret;
        if (index == 0){
            for(Node tmp : _bAllStmts){
                if(tmp instanceof MethDecl){
                    ret = tmp.getStartLine();
                    return ret;
                }
            }
            System.err.println("Can not find insert place!!!");
        }
        int line = allLines.get(index-1);
        if(new2OldLines.containsKey(line)){
            return new2OldLines.get(line);
        }
        System.err.println("Can not find insert place!!!");
        return -1;
    }

    public boolean isAncestor(Node node, Node ancestor){
        if(!hasBlock(ancestor)){
            return false;
        }
        if(node.getStmtParent() == null){
            return false;
        }
        if(!node.getFileName().equals(ancestor.getFileName())){
            return false;
        }
//        if(node instanceof ElseStmt){
//            if(node.getStmtParent() != null && node.getStmtParent() == ancestor){
//                return false;
//            }
//        }
//        StringBuilder tmp = new StringBuilder();
//        tmp.append("Current node: "+node+"; Current ancestor-> "+ancestor);
        while(node.getStmtParent() != null){
            node = node.getStmtParent();
//            tmp.append(" -> "+node);
            if(node.equals(ancestor)){
                return true;
            }
        }
//        System.out.println(tmp.toString());
        return false;
    }

    /**
     * In order to make the allSelect method can restore the original file
     * We need to delete the duplicate actions.
     * If an action's node is the sons of another action's node, then the former one is deleted.
     * @param actions: all actions
     * @return: actions without duplicate
     */
    public List<MyActions> deDuplicateActions(List<MyActions> actions){
        List<Node> insert = new ArrayList<>();
        List<MyActions> insertActions = new ArrayList<>();
        List<MyActions> ret = new ArrayList<>();

        for(MyActions action: actions){
            if(action instanceof MyInsertion || action instanceof MyDeletion){
                insert.add(action.getCurNode());
                insertActions.add(action);
            }else{
                ret.add(action);
            }
        }
        Iterator<MyActions> insertIterator = insertActions.iterator();
        while(insertIterator.hasNext()){
            MyActions ins = insertIterator.next();
            Node insNode = ins.getCurNode();
            for(Node ancestor: insert){
                if(isAncestor(insNode, ancestor)){
                    insertIterator.remove();
                    break;
                }
            }
        }
        ret.addAll(insertActions);
        Set<MyActions> tmpSet = new HashSet<>(ret);
        List<MyActions> trueRet = new ArrayList<>(tmpSet);
        trueRet.sort(Comparator.comparing(MyActions::getBaseStartLine));
        return trueRet;
    }

    /**
     * If move destination's all modified nodes are moved, then it is a wrap.
     * @return: return the wrap actions
     */
    public List<MyActions> judgeWrap(){

        Set<Node> parents = new HashSet<>();
        Set<Node> moveDstNodes = new HashSet<>();
        Map<Node, Node> dst2SrcNodes = new HashMap<>();
        Map<Node, List<Node>> parent2Modified = new HashMap<>();

        List<MyActions> retWraps = new ArrayList<>();

        for(MyActions ac:_moves){
            MyMove mv = (MyMove) ac;
            moveDstNodes.add(ac.getCurNode());
            dst2SrcNodes.put(mv.getCurNode(), mv.getFormerNode());
            // wrap should be a block, so need to find the block's parent.
            if(mv.getCurNode().getStmtParent()!= null
                    && mv.getCurNode().getStmtParent().getStmtParent() != null) {
                parents.add(mv.getCurNode().getStmtParent().getStmtParent());
            }
        }

        // Get all modified nodes' parents
        for(Node modi: _pModified){
            if(modi.getStmtParent() != null && modi.getStmtParent().getStmtParent() != null){
                Node parent = modi.getStmtParent().getStmtParent();
                if(!parents.contains(parent)){
                    continue;
                }
                if(!parent2Modified.containsKey(parent)){
                    List<Node> tmp = new ArrayList<>(); tmp.add(modi);
                    parent2Modified.put(parent, tmp);
                }else{
                    parent2Modified.get(parent).add(modi);
                }
            }
        }

        List<Integer> patchKeepLines = new ArrayList<>(_keepMap.keySet());

        // For each parent, judge if it is a wrap
        Set<Node> deletionNodes = new HashSet<>();
        Set<Node> deletionParents = new HashSet<>();
        for(Node p: parents){
            List<Node> modifies = parent2Modified.get(p);
            // if all modified nodes are moved, then it is a wrap.
            if(moveDstNodes.containsAll(modifies)){
                // find all children statements from all statemtns: modifies and keepNodes.
                List<Node> keepChildren = new ArrayList<>();
                int parentBeginLine = p.getStartLine();
                int parentEndLine = p.getEndLine();
                for(Integer patchKeepLine: patchKeepLines){
                    if(patchKeepLine >= parentBeginLine && patchKeepLine <= parentEndLine){
                        int buggyKeepLine = _keepMap.get(patchKeepLine);
                        for(Node bNode: _bAllStmts){
                            if(bNode.getStartLine() == buggyKeepLine){
                                keepChildren.add(bNode);
                            }
                        }
                    }
                }
                // Statements that been wrapped should be deleted.
                // The wrap action's Block Node should be deleted too.
                deletionNodes.addAll(modifies);
                deletionParents.add(p);
                // Get all buggy version nodes that should be deleted.
                List<Node> tmp = new ArrayList<>();
                for(Node modi: modifies){
                    tmp.add(dst2SrcNodes.get(modi));
                }
                tmp.addAll(keepChildren);
                // use a parent and its children to make a wrap action.
                if(moveDstNodes.contains(p)){
                    _node2DelNodes.put(dst2SrcNodes.get(p), tmp);
                    continue;
                }
                _node2DelNodes.put(p, tmp);
                Wrap tmpWrap = new Wrap(p, tmp);
                retWraps.add(tmpWrap);
            }
        }

        // If wrap is found, then delete the corresponding Move actions
        Iterator<MyActions> moveIterator = _moves.iterator();
        while(moveIterator.hasNext()){
            MyMove mv = (MyMove) moveIterator.next();
            if(deletionNodes.contains(mv.getCurNode())){
                moveIterator.remove();
            }
        }
        // and delete corresponding insert action
        Iterator<MyActions> actionsIterator = _actions.iterator();
        while(actionsIterator.hasNext()){
            MyActions ac = actionsIterator.next();
            if(ac instanceof MyInsertion){
                MyInsertion ins = (MyInsertion) ac;
                if(deletionParents.contains(ins.getCurNode())){
                    actionsIterator.remove();
                }
            }
        }

        return retWraps;
    }

    public boolean hasBlock(Node node){
        return node instanceof IfStmt || node instanceof ElseStmt || node instanceof TryStmt || node instanceof CatClause || node instanceof ForStmt || node instanceof WhileStmt || node instanceof EnhancedForStmt || node instanceof DoStmt || node instanceof SwitchStmt;
    }

    public Pair< Pair<Integer, Integer>, String > blockToExpr(Node formerNode,Node node){
        StringBuilder stringBuffer = new StringBuilder();
        int begin = 0;
        int end   = 0;
        if (node instanceof IfStmt){
            IfStmt tmp = (IfStmt) node;
            IfStmt former = (IfStmt) formerNode;
            stringBuffer.append("if(");
            stringBuffer.append(tmp.getCondition().toSrcString());
            stringBuffer.append("){"+Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getCondition().getEndLine();
        }
        else if(node instanceof TryStmt){
            TryStmt tmp = (TryStmt) node;
            TryStmt former = (TryStmt) formerNode;
            stringBuffer.append("try");
            List<VarDeclarationExpr> _resource = tmp.getResource();
            List<VarDeclarationExpr> _formerResource = former.getResource();
            if(_resource != null && _resource.size() > 0) {
                stringBuffer.append("(");
                stringBuffer.append(_resource.get(0).toSrcString());
                for(int i = 1; i < _resource.size(); i++) {
                    stringBuffer.append(";");
                    stringBuffer.append(_resource.get(i).toSrcString());
                }
                stringBuffer.append("){"+Constant.NEW_LINE);
            }
            begin = former.getStartLine();
            end   = _formerResource.get(_formerResource.size()-1).getEndLine();
        }
        else if(node instanceof CatClause){
            CatClause tmp = (CatClause) node;
            CatClause former = (CatClause) formerNode;
            stringBuffer.append("catch(");
            stringBuffer.append(tmp.getException().toSrcString());
            stringBuffer.append("){"+Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getException().getEndLine();
        }
        else if(node instanceof ForStmt){
            ForStmt tmp = (ForStmt) node;
            ForStmt former = (ForStmt) formerNode;
            stringBuffer.append("for(");
            ExprList _initializers = tmp.getInitializer();
            Expr _condition = tmp.getCondition();
            ExprList _updaters  = tmp.getUpdaters();
            stringBuffer.append(_initializers.toSrcString());
            stringBuffer.append(";");
            if (_condition != null) {
                stringBuffer.append(_condition.toSrcString());
            }
            stringBuffer.append(";");
            stringBuffer.append(_updaters.toSrcString());
            stringBuffer.append("){"+Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getCondition().getEndLine();
        }
        else if (node instanceof WhileStmt){
            WhileStmt tmp = (WhileStmt) node;
            WhileStmt former = (WhileStmt) formerNode;
            stringBuffer.append("while(");
            stringBuffer.append(tmp.getExpression().toSrcString());
            stringBuffer.append("){"+Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getExpression().getEndLine();
        }
        else if (node instanceof EnhancedForStmt){
            EnhancedForStmt tmp = (EnhancedForStmt) node;
            EnhancedForStmt former = (EnhancedForStmt) formerNode;
            stringBuffer.append("for(");
            stringBuffer.append(tmp.getParameter().toSrcString());
            stringBuffer.append(" : ");
            stringBuffer.append(tmp.getExpression().toSrcString());
            stringBuffer.append("){"+Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getExpression().getEndLine();
        }
        else if (node instanceof DoStmt){
            DoStmt tmp = (DoStmt) node;
            DoStmt former = (DoStmt) formerNode;
            stringBuffer.append("} while(");
            stringBuffer.append(tmp.getExpression().toSrcString());
            stringBuffer.append(");");
            begin = former.getExpression().getStartLine();
            end   = former.getExpression().getEndLine();
        }
        else if (node instanceof SwitchStmt){
            SwitchStmt tmp = (SwitchStmt) node;
            SwitchStmt former = (SwitchStmt) formerNode;
            stringBuffer.append("switch (");
            stringBuffer.append(tmp.getExpression().toSrcString());
            stringBuffer.append("){" + Constant.NEW_LINE);
            begin = former.getStartLine();
            end   = former.getExpression().getEndLine();
        }else{
            System.err.println("Unconsidered type!!! former:"+formerNode+"; after:"+ node);
        }
        Pair<Integer, Integer> lines = new Pair<>(begin, end);
        return new Pair<>(lines, stringBuffer.toString());
    }

    /**
     * Generating the actions
     */
    public void generate(){

        // If a node exists in buggy code but not matched to any node in patched code, it is a deletion
        List<Node> deleteNodes = new ArrayList<>();
        for(Node bModi: _bModified){
            if(!_bMatched.contains(bModi)){
                deleteNodes.add(bModi);
            }
        }

        // No need to deduplicate? DeleteNodes is the sub of _bModified and _bMatched
        // _bModified & _bMatched
        // deleteNodes = deDuplicate(deleteNodes);

        // Generate the deletion actions
        for(Node delNode: deleteNodes){
            MyDeletion del = new MyDeletion(delNode);
            _deltions.add(del);
        }

        // If a node is added by not matched to any node in buggy code, it is an insertion
        List<Node> insertNodes = new ArrayList<>();
        for(Node pModi: _pModified){
            if(!_pMatched.contains(pModi)){
                insertNodes.add(pModi);
            }
        }

        // Same reason, deDuplicate maybe unnecessary.
        // insertNodes = deDuplicate(insertNodes);

        // Generating Insertion actions & Move actions
        // If a node in Insertion is identical to a node in Deletion, it is a Move action.
        Iterator<Node> insIterator = insertNodes.iterator();
        while(insIterator.hasNext()){
            Node insNode = insIterator.next();
            boolean isMove = false;
            Iterator<MyActions> delIterator = _deltions.iterator();
            while (delIterator.hasNext()){
                MyActions del = delIterator.next();
                if(insNode.toString().equals(del.getCurNode().toString())){
                    _moves.add(new MyMove(del.getCurNode(), insNode));
                    delIterator.remove();
                    insIterator.remove();
                    isMove = true;
                    break;
                }
            }
            // If not a move, it is an Insertion.
            if(!isMove) {
                _insertNodes.add(insNode);
                MyInsertion ins = new MyInsertion(insNode, 0, 0);
                _insertions.add(ins);
            }
        }
        _actions.addAll(_deltions);
        _actions.addAll(_insertions);
        LevelLogger.info("Deletion and Insertion done...");

        // Generating Update actions
        // If the matched nodes' String are identical, they are a Move action.
        for(int i =0; i<_pMatched.size(); ++i){
            if(_bMatched.get(i).toString().equals(_pMatched.get(i).toString())){
                MyMove mve = new MyMove(_bMatched.get(i), _pMatched.get(i));
                _moves.add(mve);
            }else{
                Node bupdate = _bMatched.get(i);
                MyUpdate upt = new MyUpdate(_bMatched.get(i), _pMatched.get(i));
                _updateNodes.put(_pMatched.get(i), _bMatched.get(i));
                // If update nodes has block, they actually only modified the head part.
                // such as an IfStmt only modified its condition.
                // So, we need to find the head part of the block.
                if(hasBlock(bupdate)){
                    Pair<Pair<Integer, Integer>, String> ret = blockToExpr(_bMatched.get(i), _pMatched.get(i));
                    Pair<Integer, Integer> lineRange = ret.getFirst();
                    String str = ret.getSecond();
                    upt.setExprString(str);
                    upt.setStartLine(lineRange.getFirst());
                    upt.setEndLine(lineRange.getSecond());
                }
                _updates.add(upt);
            }
        }
        // Generating Wrap actions, delete corresponding Move actions.
        _wraps = judgeWrap();
        _actions.addAll(_wraps);
        LevelLogger.info("Wrap done...");
        _actions.addAll(_moves);
        _actions.addAll(_updates);
        LevelLogger.info("Updates and Moves done...");

        // For Insertion actions, we need to find the insert line.
        for(MyActions ac: _actions){
            if(ac instanceof MyInsertion){
                ((MyInsertion) ac).setLine(getInsertLineNew(ac.getCurNode()));
            }
        }
        Collections.sort(_actions, Comparator.comparing(MyActions::getBaseStartLine));
        LevelLogger.info("ACTIONS:\n"+_actions);


        // After Generated all actions, we should try to obliterate the uncessary add/delete_bracket indexes.

        LevelLogger.info("Add Bracket Lines(before):" + _addBracketLines);
        Iterator<Integer> addIterator = _addBracketLines.iterator();
        while(addIterator.hasNext()){
            int currentLine = addIterator.next();
            for (MyActions ac: _actions){
                if(ac instanceof MyDeletion) continue;
                else{
                    if( currentLine >= ac.getCurNode().getStartLine() && currentLine <= ac.getCurNode().getEndLine()){
                        addIterator.remove();
                        break;
                    }
                }
            }
        }
        LevelLogger.info("Add Bracket Lines(Final):" + _addBracketLines);

        LevelLogger.info("Delete Bracket Lines(Before):" + _deleteBracketLines);
        Iterator<Integer> delIterator = _deleteBracketLines.iterator();
        while(delIterator.hasNext()){
            int currentLine = delIterator.next();
            for(MyActions ac: _actions){
                if(ac instanceof MyDeletion){
                    if(currentLine >= ac.getCurNode().getStartLine() && currentLine <= ac.getCurNode().getEndLine()){
                        delIterator.remove();
                        break;
                    }
                }
                if(ac instanceof MyMove){
                    if(currentLine >= ((MyMove) ac).getFormerNode().getStartLine() && currentLine <= ((MyMove) ac).getFormerNode().getEndLine()){
                        delIterator.remove();
                        break;
                    }
                }
            }
        }
        LevelLogger.info("Delete Bracket Lines(Final):" + _deleteBracketLines);
    }


    /********************* Following Are Action Selection Methods***************************/

    /**
     * This method is used to apply all extracted actions.
     * By judging its differences between the original code, we can find whether the matching is correct
     * @return: all actions
     */
    public List<MyActions> allSelect(){
        List<MyActions> ret = new ArrayList<>();
        for(MyActions candidate : _actions){
            if(candidate instanceof MyInsertion){
                ret.add(candidate);
            }else if(candidate instanceof MyDeletion){
                ret.add(candidate);
            }else if(candidate instanceof MyUpdate){
                ret.add(candidate);
            }else if(candidate instanceof MyMove){
                MyMove mv = (MyMove) candidate;
                MyDeletion del = new MyDeletion(mv.getFormerNode());
                MyInsertion ins = new MyInsertion(mv.getCurNode(), mv.getStartPos(), mv.getCurStartLine());
                ins.setLine(getInsertLineNew(ins.getCurNode()));
                ret.add(del);
                ret.add(ins);
            }else if(candidate instanceof Wrap){
                // Wrap == directly insert the parent block stmt and delete the inner stmts
                Wrap wrap = (Wrap) candidate;
                MyInsertion ins = new MyInsertion(wrap.getCurNode(), 0, 0);
                ins.setLine(getInsertLineNew(ins.getCurNode()));
                ret.add(ins);
                List<Node> toDelete = wrap.getStmts();
                for(Node node: toDelete){
                    MyDeletion del = new MyDeletion(node);
                    ret.add(del);
                }
            }else{
                System.err.println("Unexpected Action!!!"); System.exit(-1);
            }
        }
        LevelLogger.info("ALL action size is:"+ret.size());
        ret = deDuplicateActions(ret);
        System.out.println(ret.size());

        System.out.println("Add Bracket Lines:" + _addBracketLines);
        // fake \} to SimpleName
        for(int line: _addBracketLines){
            int insertLine = getInsertLineNew(line);
            SName fake = new SName(null, line, line, null);
            fake.setName("}\n");
            MyInsertion ins = new MyInsertion(fake,line, insertLine);
            ret.add(ins);
        }

        ret.sort(Comparator.comparing(MyActions::getBaseStartLine));
        return ret;
    }

    public List<List<List<MyActions>>> oneByOneSelect(int limit){
        List<MyActions> oneByoneApply = new ArrayList<>();
        if(_actions.size() == 0)
            return new ArrayList<>();
        for(MyActions action: _actions){
            if(action instanceof MyInsertion&&((MyInsertion) action).getCurNode() instanceof IfStmt){
               oneByoneApply.add(action);
               _alreadyNormal.add(Collections.singletonList(_actions.indexOf(action)));
               _alreadyFiltered.add(Collections.singletonList(_actions.indexOf(action)));
            }
            if(action instanceof MyUpdate&&((MyUpdate) action).getCurNode() instanceof IfStmt){
                oneByoneApply.add(action);
                _alreadyNormal.add(Collections.singletonList(_actions.indexOf(action)));
                _alreadyFiltered.add(Collections.singletonList(_actions.indexOf(action)));
            }
        }
        List<List<List<MyActions>>> ret = new ArrayList<>();
        for(MyActions action: oneByoneApply){
            List<MyActions> tmp = new ArrayList<>();
            tmp.add(action);
            _checker.setModifications(tmp);
            List<List<MyActions>> candidates = _checker.check(limit - ret.size());

            ret.add(candidates);
        }
        LevelLogger.info("Random Select Actions...");
        if(limit > ret.size()){
            List<List<List<MyActions>>> tmp = randomSelect(limit - ret.size());
            ret.addAll(tmp);
        }
        return ret;
    }

    public List<List<MyActions>> randomCombineOriActions()
    {
        // select at most 3 actions
        int limit = Math.min(3, _actions.size());
        List<List<List<Integer>>> indexes = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
           if(indexes.isEmpty()){
               List<List<Integer>> tmp = new ArrayList<>();
               for(int j = 0; j < _actions.size(); j++){
                   List<Integer> tmp2 = new ArrayList<>();
                   tmp2.add(j);
                   tmp.add(tmp2);
               }
               indexes.add(tmp);
               continue;
           }
           List<List<Integer>> pres = indexes.get(i-1);
           List<List<Integer>> res  = new ArrayList<>();
           for(List<Integer> pre: pres){
                for(int j = pre.get(pre.size()-1)+1; j < _actions.size(); j++){
                     List<Integer> tmp = new ArrayList<>(pre);
                     tmp.add(j);
                     res.add(tmp);
                }
           }
           indexes.add(res);
        }

        // deduplicate
        List<List<MyActions>> dedup = new ArrayList<>();
        List<List<Integer>> dedupIndexes = new ArrayList<>();
        for(List<List<Integer>> tmp: indexes){
            for(List<Integer> tmp2: tmp){
                List<MyActions> tmp3 = getActionsByIndexes(tmp2);
                tmp3 = deDuplicateActions(tmp3);
                List<Integer> tmpIndex = new ArrayList<>();
                if(tmp3.size() == tmp2.size()) {
                    tmpIndex = tmp2;
                }else{
                    for(MyActions ac: tmp3){
                        tmpIndex.add(_actions.indexOf(ac));
                    }
                }
                Collections.sort(tmpIndex);
                if(!dedupIndexes.contains(tmpIndex)){
                    dedupIndexes.add(tmpIndex);
                    dedup.add(tmp3);
                }
            }
        }
        LevelLogger.info("Random Combine Actions size:"+dedup.size());
        return dedup;
    }

    // try: after each selection, check the correctness of generated patch, if correct, then return the patch.
    public List<List<List<MyActions>>> oneByoneSelectionV2(int limit){
        List<MyActions> oneByoneApply = new ArrayList<>();
        if(_actions.isEmpty())
            return new ArrayList<>();

        // insert if-stmt is the most common case
        for(MyActions action: _actions){
            if(action instanceof MyInsertion&&((MyInsertion) action).getCurNode() instanceof IfStmt){
                oneByoneApply.add(action);
                _alreadyNormal.add(Collections.singletonList(_actions.indexOf(action)));
                _alreadyFiltered.add(Collections.singletonList(_actions.indexOf(action)));
            }
            if(action instanceof MyUpdate&&((MyUpdate) action).getCurNode() instanceof IfStmt){
                oneByoneApply.add(action);
                _alreadyNormal.add(Collections.singletonList(_actions.indexOf(action)));
                _alreadyFiltered.add(Collections.singletonList(_actions.indexOf(action)));
            }
            if(action instanceof Wrap&&((Wrap) action).getCurNode() instanceof IfStmt){
                oneByoneApply.add(action);
                _alreadyNormal.add(Collections.singletonList(_actions.indexOf(action)));
                _alreadyFiltered.add(Collections.singletonList(_actions.indexOf(action)));
            }
        }

        List<List<List<MyActions>>> ret = new ArrayList<>();
        ret.add(randomCombineOriActions());
        int counter = 0;
        for(MyActions actions: oneByoneApply){
            List<MyActions> tmp2 = new ArrayList<>();
            tmp2.add(actions);
            _checker.setModifications(tmp2);
            List<List<MyActions>> candidates = _checker.check(limit - counter);
            counter += candidates.size();
            if(actions instanceof Wrap){
                List<List<MyActions>> tmpCandidates = new ArrayList<>();
                for(List<MyActions> tmp: candidates){
                    tmpCandidates.add(transActions(tmp));
                }
                candidates = tmpCandidates;
            }
            ret.add(candidates);
        }

        // if still can add other actions, then random select
        LevelLogger.info("Random Select Actions...");
        if(limit > ret.size()){
            List<List<List<MyActions>>> tmp = randomSelect(limit - ret.size());
            ret.addAll(tmp);
        }
        return ret;
    }

    /**
     * Generated random number in [0, limit)
     * @param limit: upper bound
     * @return: random number
     */
    public Integer randomNumber(int limit){
        Random sampler = new Random();
        return sampler.nextInt(limit);
    }

    /**
     * The max combination of n numbers(ordered) is 2^n - 1
     * @param n
     * @return
     */
    public int maxCombination(int n){
        int ret = (int) (Math.pow(2, n) - 1);
        return ret;
    }

    /**
     * Randomly select a number of action list
     * For each action list, number is random, select strategy is random.
     * @param number: number of action lists.
     * @return: action lists
     */
    public List<List<List<MyActions>>> randomSelect(int number){
        List<List<List<MyActions>>> retActions = new ArrayList<>();
        if(_actions.isEmpty())
            return retActions;
        int MaxCombination = maxCombination(_actions.size());
        int MaxSameSample = 1000;
        int counter = 0;
        LevelLogger.info("Random check & mutation begin:");
        int candidatesCounter = 0;
        while (candidatesCounter < number){
            // selected actions' amount is random.
            int applyNumber = randomNumber(_actions.size())+1;
            List<Integer> indexes = new ArrayList<>();
            while(indexes.size() < applyNumber){
                int index = randomNumber(_actions.size());
                if(!indexes.contains(index)){
                    indexes.add(index);
                }
            }
            Collections.sort(indexes);
            if(!_alreadyNormal.contains(indexes)){
                counter = 0;
                _alreadyNormal.add(indexes);
                LevelLogger.debug("Unfiltered actions: "+indexes);
                List<MyActions> tmp = getActionsByIndexes(indexes);
                List<MyActions> filteredActions = deDuplicateActions(tmp);
                List<Integer> filteredIndexes = new ArrayList<>();
                if(filteredActions.size() == tmp.size()){
                    filteredIndexes = indexes;
                }
                else{
                    for(MyActions ac: filteredActions){
                        filteredIndexes.add(_actions.indexOf(ac));
                    }
                }
                Collections.sort(filteredIndexes);
                if(!_alreadyFiltered.contains(filteredIndexes)){
                    LevelLogger.debug("Selected actions: "+filteredIndexes);
                    _alreadyFiltered.add(filteredIndexes);
                    _checker.setModifications(filteredActions);
                    List<List<MyActions>> candidates = _checker.check(number - candidatesCounter);
                    LevelLogger.info("Candidates size: "+candidates.size());
                    retActions.add(candidates);
                    candidatesCounter += candidates.size();
                }
            }
            counter ++;
            if(counter >= MaxSameSample){
                break;
            }
            if(_alreadyNormal.size() >= MaxCombination){
                break;
            }
        }
        if(retActions.size() > number){
            LevelLogger.info("Delete overwhelm actions...("+retActions.size()+")/("+number+")");
            retActions = retActions.subList(0, number);
        }
        LevelLogger.info("Static Check & mutation end:"+candidatesCounter);
        List<List<List<MyActions>>> ret= new ArrayList<>();
        for(List<List<MyActions>> tmp: retActions){
            List<List<MyActions>> tmpRet = new ArrayList<>();
            for(List<MyActions> tmp2: tmp){
                tmpRet.add(transActions(tmp2));
            }
            ret.add(tmpRet);
        }
        retActions.clear();
        return ret;
    }

    /**
     * Given a list of required actions' indexes, get the corresponding actions
     * @param indexes: required actions' indexes
     * @return: corresponding actions
     */
    public List<MyActions> getActionsByIndexes(List<Integer> indexes){
        List<MyActions> candidateActions = new ArrayList<>();
        for(int index: indexes){
            candidateActions.add(_actions.get(index));
        }
        return candidateActions;
    }

    /**
     * Transfer Move and Wrap ACTIONS to INSERT and DELETE ACTIONS
     * @param actions: all actions
     * @return: actions without MOVE and Wrap
     */
    public List<MyActions> transActions(List<MyActions> actions){
        List<MyActions> ret = new ArrayList<>();
        // Change all move and wrap actions to insert and delete actions
        for(MyActions candidate: actions){
            if(candidate instanceof MyInsertion){
                ret.add(candidate);
            }else if(candidate instanceof MyDeletion){
                ret.add(candidate);
            }else if(candidate instanceof MyUpdate){
                ret.add(candidate);
            }else if(candidate instanceof MyMove){
                MyMove mv = (MyMove) candidate;
                MyDeletion del = new MyDeletion(mv.getFormerNode());
                MyInsertion ins = new MyInsertion(mv.getCurNode(), mv.getStartPos(), mv.getCurStartLine());
                ins.setLine(getInsertLineNew(ins.getCurNode()));
                ret.add(del);
                ret.add(ins);
            }else if(candidate instanceof Wrap){
                // use wrap == directly insert the parent block stmt
                Wrap wrap = (Wrap) candidate;
                MyInsertion ins = new MyInsertion(wrap.getCurNode(), 0, 0);
                ins.setLine(getInsertLineNew(ins.getCurNode()));
                ret.add(ins);
                List<Node> toDelete = wrap.getStmts();
                for(Node node: toDelete){
                    MyDeletion del = new MyDeletion(node);
                    ret.add(del);
                    if(_node2DelNodes.containsKey(node)){
                        List<Node> tmp = _node2DelNodes.get(node);
                        for(Node n: tmp){
                            MyDeletion del2 = new MyDeletion(n);
                            ret.add(del2);
                        }
                    }
                }
            }else{
                System.err.println("Illegal Action!!!"); System.exit(-1);
            }
        }

        // do a deduplicated?
        // if insert a stmt, and its parent is also inserted, this change is illegal:
        // + if ( a > 1 ){
        // +     return 1;
        // +}
        // + return 1; and if ( a > 1 ){ return 1; } should not appear at the same time.

        // update: not necessary, update will not exclude each other
        // insert: necessary, insert will exclude each other
        // delete: necessary, delete will exclude each other

        for(int line: _addBracketLines){
            int insertLine = getInsertLineNew(line);
            SName fake = new SName("fake", line, line, null);
            fake.setName("}\n");
            MyInsertion ins = new MyInsertion(fake,line, insertLine);
            ret.add(ins);
        }

        ret = deDuplicateActions(ret);
        return ret;
    }

    public List<Stmt> expandChildren(Node node){
        if(node instanceof Blk){
           return ((Blk) node).getStatement();
        }else if(node instanceof ForStmt){
            List<Stmt> ret = new ArrayList<>();
            ForStmt tmp = (ForStmt) node;
            if(tmp.getBody() instanceof Blk){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }else{
                ret.add(tmp.getBody());
            }
            return ret;
        }else if(node instanceof CatClause){
            List<Stmt> ret = new ArrayList<>();
            CatClause tmp = (CatClause) node;
            if(tmp.getBody() != null){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }
            return ret;
        }else if(node instanceof DoStmt){
            List<Stmt> ret = new ArrayList<>();
            DoStmt tmp = (DoStmt) node;
            if(tmp.getBody() instanceof Blk){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }else{
                ret.add(tmp.getBody());
            }
            return ret;
        }else if(node instanceof EnhancedForStmt){
            List<Stmt> ret = new ArrayList<>();
            EnhancedForStmt tmp = (EnhancedForStmt) node;
            if(tmp.getBody() instanceof Blk){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }else{
                ret.add(tmp.getBody());
            }
            return ret;
        }else if(node instanceof IfStmt){
            List<Stmt> ret = new ArrayList<>();
            IfStmt tmp = (IfStmt) node;
            if(tmp.getThen() instanceof Blk){
                ret.addAll(((Blk) tmp.getThen()).getStatement());
            }else{
                ret.add(tmp.getThen());
            }
            if(tmp.getElse() != null && tmp.getElse() instanceof Blk){
                ret.addAll(((Blk) tmp.getElse()).getStatement());
            }else if(tmp.getElse() != null){
                ret.add(tmp.getElse());
            }
            return ret;
        }else if(node instanceof SwitchStmt){
            List<Stmt> ret = new ArrayList<>();
            SwitchStmt tmp = (SwitchStmt) node;
            for(Stmt stmt: tmp.getStatements()){
                if(stmt instanceof Blk){
                    ret.addAll(((Blk) stmt).getStatement());
                }else{
                    ret.add(stmt);
                }
            }
            return ret;
        }else if(node instanceof SynchronizedStmt){
            List<Stmt> ret = new ArrayList<>();
            SynchronizedStmt tmp = (SynchronizedStmt) node;
            if(tmp.getBody() != null){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }
            return ret;
        }else if(node instanceof TryStmt){
            List<Stmt> ret = new ArrayList<>();
            TryStmt tmp = (TryStmt) node;
            if(tmp.getBody() != null){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }
            if(tmp.getCatches() != null){
                for(CatClause cat: tmp.getCatches()){
                    if(cat.getBody() != null){
                        ret.addAll(((Blk) cat.getBody()).getStatement());
                    }
                }
            }
            if(tmp.getFinally() != null){
                ret.addAll(((Blk) tmp.getFinally()).getStatement());
            }
            return ret;
        }else if(node instanceof WhileStmt){
            List<Stmt> ret = new ArrayList<>();
            WhileStmt tmp = (WhileStmt) node;
            if(tmp.getBody() instanceof Blk){
                ret.addAll(((Blk) tmp.getBody()).getStatement());
            }else{
                ret.add(tmp.getBody());
            }
            return ret;
        }
        return new ArrayList<>();
    }
}
