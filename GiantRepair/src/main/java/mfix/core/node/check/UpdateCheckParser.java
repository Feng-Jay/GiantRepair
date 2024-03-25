package mfix.core.node.check;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.*;
import mfix.core.node.ast.expr.*;
import mfix.core.node.match.metric.LevenShteinDistance;

import java.util.*;

public class UpdateCheckParser {

    // parser to mutation diff part of update check
    private final CheckParser _checkParser;

    private int MAXSTMTRETURNSIZE = 1000;
    private int MAXSUBMEMBERSIZE  = 500;

    public UpdateCheckParser(CheckParser checkParser) {
        _checkParser = checkParser;
    }

    public List<Integer> diffOfListElems(List<Node> oriNodes, List<Node> mutNodes){
        Set<String> oriNodesString = new HashSet<>();
        for(Node node: oriNodes){
            oriNodesString.add(node.toString());
        }

        List<Integer> ret = new ArrayList<>();
        for(int i = 0; i < mutNodes.size(); ++i){
            if(!oriNodesString.contains(mutNodes.get(i).toString())){
                ret.add(i);
            }
        }
        return ret;
    }

    /**
     * Compute candidates' similarity with mut node and rank/filter them.
     * @param mut: mutation node.
     * @param candidates: candidates node.
     * @return: ranked and filtered candidates.
     */
    public List<Node> rankAndFilterBySimilarityStmt(Node mut, List<Node> candidates){
        String mutString = mut.toString();
        int MaxCandidatesLength = 10000;

        if(candidates.size() > MaxCandidatesLength){
            candidates = candidates.subList(0, MaxCandidatesLength);
        }
        for(Node candidate: candidates){
            String candidateString = candidate.toString();
            LevenShteinDistance calculator = new LevenShteinDistance(mutString, candidateString);
            candidate.setSimilarity((calculator.compute()*1.0) / (mutString.length()*1.0));
        }
        candidates.sort(new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                return Double.compare(o1.getSimilarity(), o2.getSimilarity());
            }
        });
        if(candidates.get(0).getSimilarity() == 0.0 && candidates.get(candidates.size()-1).getSimilarity() == 0.0){
            LevelLogger.error("All candidates' similarity are 0.0, please check!!!");
        }
        Set<String> already = new HashSet<>();
        List<Node> ret = new ArrayList<>();
        int counter = 0;
        for(Node candidate: candidates){
            counter ++;
            if(candidate instanceof MethodInv){
                MethodInv tmp = (MethodInv) candidate;
//                if(tmp.getName().toString().equals("isGetOrSetKey")){
//                    System.err.println("isGetOrSetKey index:"+counter);
//                }
            }
            if(!already.contains(candidate.toString())){
                already.add(candidate.toString());
                ret.add(candidate);
            }
        }
        if(mut instanceof Stmt){
            MaxCandidatesLength = MAXSTMTRETURNSIZE;
        }else{
            MaxCandidatesLength = MAXSUBMEMBERSIZE;
        }
        if(ret.size() > MaxCandidatesLength){
            ret = ret.subList(0, MaxCandidatesLength);
        }
        return ret;
    }

    /**
     * extract ori and mut's diff then do mutation.
     * @param ori: original node
     * @param mut: mutated node
     * @param type: mutation type
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(AssertStmt ori, AssertStmt mut, String type){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Ignore this case, update can not include a block.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(Blk ori, Blk mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * BreakStmt can not be mutated.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(BreakStmt ori, BreakStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }


    /**
     * For CatClause, check the svd part.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(CatClause ori, CatClause mut){
        Expr oriSvd = ori.getException();
        Expr mutSvd = mut.getException();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> svdCandidates = new ArrayList<>();
        if(!oriSvd.toString().equals(mutSvd.toString())) {
            svdCandidates = process(oriSvd, mutSvd, "");
        }
        for(Node candidate: svdCandidates){
            CatClause tmp = new CatClause(mut);
            tmp.addTrace("UPDATECatClause");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setException((Svd) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * ConsturctorInvocation node is skipped.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ConstructorInv ori, ConstructorInv mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        if(!ori.toString().equals(mut.toString())){
            List<Node> candidates = _checkParser.process(mut, "");
            ret.addAll(candidates);
        }
        return ret;
    }

    /**
     * ContinueStmt node is skipped.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ContinueStmt ori, ContinueStmt mut){
       List<Node> ret = new ArrayList<>();
       ret.add(mut);
       return ret;
    }

    /**
     * Check the while's condition.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(DoStmt ori, DoStmt mut){
        Expr oriCondition = ori.getExpression();
        Expr mutCondition = mut.getExpression();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> conditionCandidates = new ArrayList<>();
        if(!oriCondition.toString().equals(mutCondition.toString())){
            conditionCandidates = process(oriCondition, mutCondition, "Boolean");
        }
        for(Node candidate: conditionCandidates){
            DoStmt tmp = new DoStmt(mut);
            tmp.addTrace("UPDATEDoStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            tmp.setBody(mut.getBody());
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip ElseStmt node
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ElseStmt ori, ElseStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip EmptyStmt node
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(EmptyStmt ori, EmptyStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip EnhancedForStmt node
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(EnhancedForStmt ori, EnhancedForStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * ExpressionStmt node, just pass them.
     * @param ori: original node
     * @param mut: mutated node
     * @param type: mutation type
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ExpressionStmt ori, ExpressionStmt mut, String type){
        Expr oriExpression = ori.getExpression();
        Expr mutExpression = mut.getExpression();
        List<Node> candidates = rankAndFilterBySimilarityStmt(mut, process(oriExpression, mutExpression, type));
        List<Node> ret = new ArrayList<>();
        for (Node candidate: candidates){
            ExpressionStmt tmp = new ExpressionStmt(mut);
            tmp.addTrace("UPDATEExpressionStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        return ret;
    }

    /**
     * ForStmt node, check the condition part.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ForStmt ori, ForStmt mut){
        Expr oriCondition = ori.getCondition();
        Expr mutCondition = mut.getCondition();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> conditionCandidates = new ArrayList<>();
        if(!oriCondition.toString().equals(mutCondition.toString())){
            conditionCandidates = process(oriCondition, mutCondition, "Boolean");
        }
        for(Node candidate: conditionCandidates){
            ForStmt tmp = new ForStmt(mut);
            tmp.addTrace("UPDATEForStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setCondition((Expr) candidate);
            tmp.setBody(mut.getBody());
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * IfStmt node, check the condition part.
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(IfStmt ori, IfStmt mut){
        Expr oriCondition = ori.getCondition();
        Expr mutCondition = mut.getCondition();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> conditionCandidates = new ArrayList<>();
        if(!oriCondition.toString().equals(mutCondition.toString())){
            conditionCandidates = process(oriCondition, mutCondition, "Boolean");
        }
        for(Node candidate: conditionCandidates){
            IfStmt tmp = new IfStmt(mut);
            tmp.addTrace("UPDATEIfStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setCondition((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip LabeledStmt node
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(LabeledStmt ori, LabeledStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate ReturnStmt's expression part
     * @param ori: original node
     * @param mut: mutated node
     * @return: list of nodes that need to be checked
     */
    public List<Node> check(ReturnStmt ori, ReturnStmt mut){
        Expr oriExpr = ori.getExpression();
        Expr mutExpr = mut.getExpression();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> exprCandidates = new ArrayList<>();
        if(!oriExpr.toString().equals(mutExpr.toString())){
            exprCandidates = process(oriExpr, mutExpr, "");
        }
        for(Node candidate: exprCandidates){
            ReturnStmt tmp = new ReturnStmt(mut);
            tmp.addTrace("UPDATEReturnStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip SuperConstructorInv node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SuperConstructorInv ori, SuperConstructorInv mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate SwitchCase's expression part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SwCase ori, SwCase mut){
        Expr oriExpression = ori.getExpression();
        Expr mutExpression = mut.getExpression();
        List<Node> exprCandidates = new ArrayList<>();
        if(!oriExpression.toString().equals(mutExpression.toString())){
            exprCandidates = process(oriExpression, mutExpression, "");
        }
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        for(Node candidate: exprCandidates){
            SwCase tmp = new SwCase(mut);
            tmp.addTrace("UPDATESwCase");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Mutate SwitchStmt's expression part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SwitchStmt ori, SwitchStmt mut){
        Expr oriExpression = ori.getExpression();
        Expr mutExpression = mut.getExpression();
        List<Node> exprCandidates = new ArrayList<>();
        if(!oriExpression.toString().equals(mutExpression.toString())){
            exprCandidates = process(oriExpression, mutExpression, "");
        }
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        for(Node candidate: exprCandidates){
            SwitchStmt tmp = new SwitchStmt(mut);
            tmp.addTrace("UPDATESwitchStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip SynchronizedStmt node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SynchronizedStmt ori, SynchronizedStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate ThrowStmt's expression part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(ThrowStmt ori, ThrowStmt mut){
        Expr oriExpression = ori.getExpression();
        Expr mutExpression = mut.getExpression();
        List<Node> exprCandidates = new ArrayList<>();
        if(!oriExpression.toString().equals(mutExpression.toString())){
            exprCandidates = process(oriExpression, mutExpression, "");
        }
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        for(Node candidate: exprCandidates){
            ThrowStmt tmp = new ThrowStmt(mut);
            tmp.addTrace("UPDATEThrowStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip TryStmt node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(TryStmt ori, TryStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip TypeDeclarationStmt node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(TypeDeclarationStmt ori, TypeDeclarationStmt mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }



    /**
     * Mutate the VarDeclarationStmt's fragments part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(VarDeclarationStmt ori, VarDeclarationStmt mut){
        List<Vdf> oriVdfs = ori.getFragments();
        List<Vdf> mutVdfs = mut.getFragments();
        String type = ori.getDeclType().getTypeStr();

        List<Node> oriVdfTmp = new ArrayList<>(oriVdfs);
        List<Node> mutVdfTmp = new ArrayList<>(mutVdfs);
        List<Integer> diffIndexes = diffOfListElems(oriVdfTmp, mutVdfTmp);

        Queue<List<Vdf>> building = new LinkedList<>();
        for(int i =0; i< mutVdfs.size(); ++i){
            List<Node> candidates = new ArrayList<>();
            if(diffIndexes.contains(i)){
                candidates = _checkParser.process(mutVdfs.get(i), "");
            }
            else{
                candidates.add(mutVdfs.get(i));
            }
            if(building.isEmpty()){
                for(Node candidate: candidates){
                    List<Vdf> tmp = new ArrayList<>();
                    tmp.add((Vdf) candidate);
                    building.add(tmp);
                }
            }else{
                Queue<List<Vdf>> pres = new LinkedList<>();
                pres.addAll(building);
                building.clear();
                while(!pres.isEmpty()){
                    List<Vdf> pre = pres.poll();
                    for(Node candidate: candidates){
                        List<Vdf> tmp = new ArrayList<>(pre);
                        tmp.add((Vdf) candidate);
                        building.add(tmp);
                    }
                }
            }
        }
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        for(List<Vdf> vdfs: building){
            VarDeclarationStmt tmp = new VarDeclarationStmt(mut);
            tmp.addTrace("UPDATEVarDeclarationStmt");
            for(Vdf vdf: vdfs){
                tmp.addAllTrace(vdf.getTrace());
            }
            tmp.setFragments(vdfs);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Mutate WhileStmt's condition part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(WhileStmt ori, WhileStmt mut){
        Expr oriCondition = ori.getExpression();
        Expr mutCondition = mut.getExpression();
        List<Node> ret = new ArrayList<>();
        List<Node> conditionCandidates = new ArrayList<>();
        if(!oriCondition.toString().equals(mutCondition.toString())){
            conditionCandidates = process(oriCondition, mutCondition, "Boolean");
        }
        for(Node candidate: conditionCandidates){
            WhileStmt tmp = new WhileStmt(mut);
            tmp.addTrace("UPDATEWhileStmt");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            tmp.setBody(mut.getBody());
            ret.add(tmp);
        }
        ret.add(mut);
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    //**********************************Following are Expression Check Methods***************************************

    /**
     * Check AryAcc's index part
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(Expr ori, AryAcc mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate AryCreation node's Init part
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(Expr ori, AryCreation mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip AryInitializer node
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(Expr ori, AryInitializer mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }


    /**
     * Mutate Assign's rhs for now
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(Assign ori, Assign mut){
        Expr oriRhs = ori.getRhs();
        Expr mutRhs = mut.getRhs();
        List<Node> rhsCandidates = new ArrayList<>();
        if(!oriRhs.toString().equals(mutRhs.toString())){
            rhsCandidates = process(oriRhs, mutRhs, "");
        }
        if(rhsCandidates.size() == 1){
            rhsCandidates.addAll(_checkParser.process(mutRhs, ""));
        }
        List<Node> ret = new ArrayList<>();
        for(Node candidate: rhsCandidates){
            Assign tmp = new Assign(mut);
            tmp.addTrace("UPDATEAssign");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setLeftHandSide(mut.getLhs());
            tmp.setRightHandSide((Expr) candidate);
            ret.add(tmp);
        }
        ret.add(mut);
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Mutate AssignOperator
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(AssignOperator ori, AssignOperator mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(BoolLiteral ori, BoolLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(Expr ori, CastExpr mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(CharLiteral ori, CharLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(ClassInstCreation ori, ClassInstCreation mut){
        List<Node> ret = new ArrayList<>();
        if(!ori.toSrcString().toString().equals(mut.toSrcString().toString())){
            ret = _checkParser.process(mut, "");
        }else{
            ret.add(mut);
        }
        return ret;
    }

    public List<Node> check(ConditionalExpr ori, ConditionalExpr mut){
        Expr oriCondition = ori.getCondition();
        Expr oriFirst = ori.getfirst();
        Expr oriSecond = ori.getSecond();

        Expr mutCondition = mut.getCondition();
        Expr mutFirst = mut.getfirst();
        Expr mutSecond = mut.getSecond();

        List<Node> conditionCandidates = process(oriCondition, mutCondition, "Boolean");
        List<Node> firstCandidates = process(oriFirst, mutFirst, "");
        List<Node> secondCandidates = process(oriSecond, mutSecond, "");

        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        if(Constant.COMBINATION_OPTION){
            for(Node conditionCandidate: conditionCandidates){
                for(Node firstCandidate: firstCandidates){
                    for(Node secondCandidate: secondCandidates){
                        ConditionalExpr tmp = new ConditionalExpr(mut);
                        tmp.setCondition((Expr) conditionCandidate);
                        tmp.setFirst((Expr) firstCandidate);
                        tmp.setSecond((Expr) secondCandidate);
                        ret.add(tmp);
                    }
                }
            }
        }else{
            for(Node conditionCandidate: conditionCandidates){
                ConditionalExpr tmp = new ConditionalExpr(mut);
                tmp.addTrace("UPDATEConditionalExpr");
                tmp.addAllTrace(conditionCandidate.getTrace());
                tmp.setCondition((Expr) conditionCandidate);
                tmp.setFirst(mutFirst);
                tmp.setSecond(mutSecond);
                ret.add(tmp);
            }
            for(Node firstCandidate: firstCandidates){
                ConditionalExpr tmp = new ConditionalExpr(mut);
                tmp.addTrace("UPDATEConditionalExpr");
                tmp.addAllTrace(firstCandidate.getTrace());
                tmp.setCondition(mutCondition);
                tmp.setFirst((Expr) firstCandidate);
                tmp.setSecond(mutSecond);
                ret.add(tmp);
            }
            for(Node secondCandidate: secondCandidates){
                ConditionalExpr tmp = new ConditionalExpr(mut);
                tmp.addTrace("UPDATEConditionalExpr");
                tmp.addAllTrace(secondCandidate.getTrace());
                tmp.setCondition(mutCondition);
                tmp.setFirst(mutFirst);
                tmp.setSecond((Expr) secondCandidate);
                ret.add(tmp);
            }
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    public List<Node> check(Expr ori, CreationRef mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(DoubleLiteral ori, DoubleLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(Expr ori, ExpressionMethodRef mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Handle only one layer access situation, like: a.b
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(FieldAcc ori ,FieldAcc mut, String type){
        Expr oriField = ori.getExpression();
        Expr oriName = ori.getIdentifier();

        Expr mutField = mut.getExpression();
        Expr mutName = mut.getIdentifier();

        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> fieldCandidates = new ArrayList<>();
        List<Node> nameCandidates = new ArrayList<>();
        // if field is too complex, just return mut
        if(oriField.toString().split("\\.").length > 1){
            ret.add(mut);
            return ret;
        }
        // if field is different, just call INSERT Checker.
        if(!mutField.toString().equals(oriField.toString())){
            ret = _checkParser.process(mut, type);
            return ret;
        }
        // if field is same, check different names.
        if(!mutName.toString().equals(oriName.toString())){
            if(oriField instanceof SName){
                List<Variable> usableVars = _checkParser.findVar(oriField.toString(), type, mutName.getStartLine());
                if(usableVars != null){
                    for (Variable usableVar : usableVars) {
                        SName tmpVar = new SName(mutName.getFileName(), mutName.getStartLine(), mutName.getEndLine(), mutName.getOriNode());
                        tmpVar.setName(usableVar.getName());
                        FieldAcc tmp = new FieldAcc(mut);
                        tmp.setIdentifier(tmpVar);
                        tmp.addTrace("UPDATEFieldAcc");
                        ret.add(tmp);
                    }
                    return ret;
                }
            }
        }
        ret.add(mut);
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip FloatLiteral node
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(FloatLiteral ori, FloatLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    private List<Node> expandHelper(Expr sideExpr){
        List<Node> ret = new ArrayList<>();
        if(sideExpr instanceof ParenthesiszedExpr){
            ParenthesiszedExpr tmp = (ParenthesiszedExpr) sideExpr;
            ret.addAll(expandHelper(tmp.getExpression()));
        }else if(sideExpr instanceof InfixExpr && (((InfixExpr) sideExpr).getOperator().toString().equals("&&") || ((InfixExpr) sideExpr).getOperator().toString().equals("||"))){
            InfixExpr tmp = (InfixExpr) sideExpr;
            ret.addAll(expandHelper(tmp.getRhs()));
            ret.addAll(expandHelper(tmp.getLhs()));
        }else{
            ret.add(sideExpr);
        }
        return ret;
    }

    public List<Node> expandInfixExprs(Expr expr){
        List<Node> elems = new ArrayList<>();
        List<Node> ops =new ArrayList<>();
        InfixExpr tmpExpr = (InfixExpr) expr;
        if(!tmpExpr.getOperator().toString().equals("&&") && !tmpExpr.getOperator().toString().equals("||")){
            elems.add(expr);
            return elems;
        }
        Expr rhs = tmpExpr.getRhs();
        elems.addAll(expandHelper(rhs));
        Expr lhs = tmpExpr.getLhs();
        elems.addAll(expandHelper(lhs));
//            Expr lhs = tmp.getLhs();
//            LevelLogger.debug("LEFT"+lhs);
//            if(lhs instanceof ParenthesiszedExpr) lhs = ((ParenthesiszedExpr) lhs).getExpression();
//            if(lhs instanceof InfixExpr &&(((InfixExpr)lhs).getOperator().toString().equals("&&") || ((InfixExpr)lhs).getOperator().toString().equals("||"))){
//                candidates.add((InfixExpr) lhs);
//            }else{
//                elems.add(lhs);
//            }
        Collections.reverse(elems);
        LevelLogger.debug("elems:"+elems);
        return elems;
    }

    public List<Node> randomCombine(List<Node> elems, InfixOperator op, Expr expr){
        Queue<List<Integer>> indexes = new LinkedList<>();
        for(int i = 0; i < elems.size(); ++i){
            if(indexes.isEmpty()){
                List<Integer> tmp = new ArrayList<>();
                tmp.add(0);indexes.add(tmp);
                List<Integer> tmp1 = new ArrayList<>();
                tmp1.add(1);indexes.add(tmp1);
            }else{
                List<List<Integer>> pres = new ArrayList<>(indexes);
                indexes.clear();
                for(List<Integer> pre: pres){
                    List<Integer> tmp = new ArrayList<>(pre);
                    tmp.add(0);indexes.add(tmp);
                    List<Integer> tmp1 = new ArrayList<>(pre);
                    tmp1.add(1);indexes.add(tmp1);
                }
            }
        }
        LevelLogger.debug("Indexes:"+indexes);
        List<Node> ret = new ArrayList<>();
        for(List<Integer> index: indexes){
            List<Expr> exprs = new ArrayList<>();
            LevelLogger.debug("Index:"+index);
            for(int i=0; i<index.size(); ++i){
                if(index.get(i) == 1) exprs.add((Expr) elems.get(i));
            }
            if(exprs.isEmpty() || exprs.size() == elems.size()) continue;
            if(exprs.size() == 1){
                ret.add(exprs.get(0));
                continue;
            }
            Expr tmp = exprs.get(0);
            LevelLogger.debug("Exprs:"+exprs);
            for(int i=1; i<exprs.size(); ++i){
                InfixExpr tmpInfix = new InfixExpr(((InfixExpr) expr));
                tmpInfix.setRightHandSide(tmp);
                tmpInfix.setLeftHandSide(exprs.get(i));
                tmp = tmpInfix;
            }
            LevelLogger.debug("Add tmp:"+tmp);
            ret.add(tmp);
        }
        LevelLogger.debug("RandomCombineOfIfCondition: "+ret);
        return ret;
    }

    /**
     * When it comes to InfixExpr, we need check each part of it.
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(Expr ori, InfixExpr mut, String type){
        if(ori instanceof InfixExpr && type.equals("Boolean")){
            List<Node> oriElems = expandInfixExprs(ori);
            List<String> oriStrs = new ArrayList<>();
            for(Node elem: oriElems) oriStrs.add(elem.toString());

            List<Node> mutElems = expandInfixExprs(mut);
            List<String> mutStrs = new ArrayList<>();
            for(Node elem: mutElems) mutStrs.add(elem.toString());

            List<Integer> newAdds = new ArrayList<>();
            for(int i = 0; i < mutStrs.size(); ++i){
                if(!(oriStrs.contains(mutStrs.get(i)))){
                    newAdds.add(i);
                }
            }
            // 1. random combine them
            List<Node> allCandidates = new ArrayList<>(mutElems);
            List<Node> ret = randomCombine(allCandidates, (InfixOperator) mut.getOperator(), mut);

            // 2. muatation each of them
            if(Constant.COMBINATION_OPTION){
                Queue<Expr> building = new LinkedList<>();
                for(int i=0; i< mutElems.size(); ++i){
                    List<Node> candidates;
                    if(newAdds.contains(i)){
                        candidates = _checkParser.process((Expr) mutElems.get(i), type);
                    }else{
                        candidates = new ArrayList<>();
                        candidates.add(mutElems.get(i));
                    }
                    if(building.isEmpty()){
                        for(Node candidate: candidates){
                            building.add((Expr) candidate);
                        }
                    }else{
                        Queue<Expr> pres = new LinkedList<>();
                        pres.addAll(building);
                        building.clear();
                        while(!pres.isEmpty()){
                            Expr pre = pres.poll();
                            for(Node candidate: candidates){
                                InfixExpr tmp = new InfixExpr(mut);
                                tmp.setOperator(mut.getOperator());
                                tmp.setLeftHandSide(pre);
                                tmp.setRightHandSide((Expr) candidate);
                                building.add(tmp);
                            }
                        }
                    }
                }
                ret.addAll(building);
            }
            else{
                List<Node> tmpRet = new ArrayList<>();
                for(int i=0; i<mutElems.size(); ++i){
                    List<Node> candidates;
                    if(newAdds.contains(i)){
                        candidates = _checkParser.process((Expr) mutElems.get(i), type);
                    }else{
                        candidates = new ArrayList<>();
                        candidates.add(mutElems.get(i));
                    }
                    Queue<Expr> building = new LinkedList<>();
                    for(int j =0; j< mutElems.size(); ++j){
                        List<Node> currentCandidates;
                        if(j == i) {
                            currentCandidates = candidates;
                        }else{
                            currentCandidates = new ArrayList<>();
                            currentCandidates.add(mutElems.get(j));
                        }
                        if(building.isEmpty()) {
                            for (Node candidate : currentCandidates) {
                                building.add((Expr) candidate);
                            }
                        }else{
                            Queue<Expr> pres = new LinkedList<>();
                            pres.addAll(building);
                            building.clear();
                            while(!pres.isEmpty()){
                                Expr pre = pres.poll();
                                for(Node candidate: currentCandidates){
                                    if(mutElems.size() > 3){
                                        InfixExpr tmp = new InfixExpr(mut);
                                        tmp.setOperator(mut.getOperator());
                                        tmp.setLeftHandSide(pre);
                                        tmp.setRightHandSide((Expr) candidate);
                                        building.add(tmp);
                                        continue;
                                    }
                                    InfixOperator tmp1 = new InfixOperator(mut.getFileName(), mut.getStartLine(), mut.getEndLine(), mut.getOriNode());
                                    tmp1.setOperatorStr("||");
                                    InfixOperator tmp2 = new InfixOperator(mut.getFileName(), mut.getStartLine(), mut.getEndLine(), mut.getOriNode());
                                    tmp2.setOperatorStr("&&");

                                    InfixExpr tmp = new InfixExpr(mut);
                                    tmp.setLeftHandSide(pre);
                                    tmp.setOperator(tmp1);
                                    tmp.setRightHandSide((Expr) candidate);
                                    building.add(tmp);

                                    InfixExpr tmp3 = new InfixExpr(mut);
                                    tmp3.setLeftHandSide(pre);
                                    tmp3.setOperator(tmp2);
                                    tmp3.setRightHandSide((Expr) candidate);
                                    building.add(tmp3);
                                }
                            }
                        }
                    }
                    for(int j = 0; j < building.size(); ++j){
                        Expr tmp = building.poll();
                        tmp.addTrace("UPDATEInfixExpr");
                        tmpRet.add(tmp);
                    }
                }
                ret.add(mut);
                ret.addAll(rankAndFilterBySimilarityStmt(mut, tmpRet));
            }
            return ret;
        }
        else if(ori instanceof MethodInv && type.equals("Boolean")){
            List<Node> mutElems = expandInfixExprs(mut);
            List<String> mutStrs = new ArrayList<>();
            for(Node elem: mutElems) mutStrs.add(elem.toString());

            List<Integer> newAdds = new ArrayList<>();
            for(int i = 0; i < mutStrs.size(); ++i){
                if(!(ori.toString().equals(mutStrs.get(i)))){
                    newAdds.add(i);
                }
            }
            Queue<Expr> building = new LinkedList<>();
            for(int i=0; i< mutElems.size(); ++i){
                List<Node> candidates;
                if(newAdds.contains(i)){
                    candidates = _checkParser.process((Expr) mutElems.get(i), type);
                }else{
                    candidates = new ArrayList<>();
                    candidates.add(mutElems.get(i));
                }
                if(building.isEmpty()){
                    for(Node candidate: candidates){
                        building.add((Expr) candidate);
                    }
                }else{
                    Queue<Expr> pres = new LinkedList<>();
                    pres.addAll(building);
                    building.clear();
                    while(!pres.isEmpty()){
                        Expr pre = pres.poll();
                        for(Node candidate: candidates){
                            InfixExpr tmp = new InfixExpr(mut);
                            tmp.setOperator(mut.getOperator());
                            tmp.setLeftHandSide(pre);
                            tmp.setRightHandSide((Expr) candidate);
                            tmp.addTrace("UPDATEInfixExpr");
                            building.add(tmp);
                        }
                    }
                }
            }
            List<Node> ret = new ArrayList<>(building);
            ret.add(mut);
            return rankAndFilterBySimilarityStmt(mut, ret);
        }
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip InfixOperator node
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(InfixOperator ori, InfixOperator mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip InstanceofExpr node
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(InstanceofExpr ori, InstanceofExpr mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip IntLiteral node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(IntLiteral ori, IntLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip LambdaExpr node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(LambdaExpr ori, LambdaExpr mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip LongLiteral node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(LongLiteral ori, LongLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * if two methodInv is not same, call INSERT checker.
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(MethodInv ori, MethodInv mut, String type){
//        LevelLogger.debug("UpdateCheck MethodInv:"+ori.toSrcString().toString()+" "+mut.toSrcString().toString());
        List<Node> ret = new ArrayList<>();
        if(!ori.toSrcString().toString().equals(mut.toSrcString().toString())){
            ret = _checkParser.process(mut, type);
        }else{
            ret.add(mut);
        }
//        LevelLogger.debug("UpdateCheck MethodInv Done!");
//        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip MethodRef node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(MethodRef ori, MethodRef mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip Mtype node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(MType ori, MType mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip NillLiteral node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(NillLiteral ori, NillLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip NumberLiteral node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(NumLiteral ori, NumLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Check the expr part of ParenthesiszedExpr node.
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(ParenthesiszedExpr ori, ParenthesiszedExpr mut, String type){
        Expr oriExpr = ori.getExpression();
        Expr mutExpr = mut.getExpression();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Node> exprCandidates = new ArrayList<>();
        if(!oriExpr.toString().equals(mutExpr.toString())){
            exprCandidates = process(oriExpr, mutExpr, type);
        }
        for(Node candidate: exprCandidates){
            ParenthesiszedExpr tmp = new ParenthesiszedExpr(mut);
            tmp.setExpr((Expr) candidate);
            tmp.addTrace("UPDATEParenthesiszedExpr");
            tmp.addAllTrace(candidate.getTrace());
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip all postfixExprs & operators, in java, only two situations: i++, i--
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(PostfixExpr ori, PostfixExpr mut, String type){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip PostOperator node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(PostOperator ori, PostOperator mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate PrefixExpr's expr part.
     * ++, --, +, -, !, ~
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(PrefixExpr ori, PrefixExpr mut, String type){
        Expr oriExpr = ori.getExpression();
        Expr mutExpr = mut.getExpression();
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        if(oriExpr.toString().equals(mutExpr.toString())){
            return ret;
        }
        List<Node> exprCandidates = process(oriExpr, mutExpr, type);
        for(Node candidate: exprCandidates){
            PrefixExpr tmp = new PrefixExpr(mut);
            tmp.addTrace("UPDATEPrefixExpr");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Ignore PrefixOperator node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(PrefixOperator ori, PrefixOperator mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Mutate QualifiedName Node
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(QName ori, QName mut, String type){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        if(!ori.toString().equals(mut.toString())){
            ret = _checkParser.process(mut, type);
            ret.add(mut);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Mutate SimpleName node
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> check(SName ori, SName mut, String type){
        List<Node> ret = new ArrayList<>();
        if(!ori.toString().equals(mut.toString())){
            ret = _checkParser.process(mut, type);
            ret.add(mut);
        }else{
            ret.add(mut);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip StringLiteral node
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(StrLiteral ori, StrLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip SuperFieldAcc node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SuperFieldAcc ori, SuperFieldAcc mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip SuperMethodInv node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SuperMethodInv ori, SuperMethodInv mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip SuperMethodRef node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(SuperMethodRef ori, SuperMethodRef mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(Svd ori, Svd mut){
        Expr oriInitializer = ori.getInitializer();
        Expr mutInitializer = mut.getInitializer();

        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        if(oriInitializer == null || mutInitializer == null){
            return ret;
        }
        if(!oriInitializer.toString().equals(mutInitializer.toString())) {
            ret = _checkParser.process(mut, "");
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Skip ThisExpr node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(ThisExpr ori, ThisExpr mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip TyLiteral node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(TyLiteral ori, TyLiteral mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    /**
     * Skip TypeMethodRef node.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(TypeMethodRef ori, TypeMethodRef mut){
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        return ret;
    }

    public List<Node> check(VarDeclarationExpr ori, VarDeclarationExpr mut){
        List<Vdf> oriFragments = ori.getFragments();
        List<Node> oriFragmentsTmp = new ArrayList<>(oriFragments);
        List<Vdf> mutFragments = mut.getFragments();
        List<Node> mutFragmentsTmp = new ArrayList<>(mutFragments);

        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        List<Integer> diffFragmentsIndexes = diffOfListElems(oriFragmentsTmp, mutFragmentsTmp);

        Queue<List<Vdf>> building = new LinkedList<>();
        for(int i = 0; i< mutFragments.size(); ++i){
            List<Node> candidates = new ArrayList<>();
            if(diffFragmentsIndexes.contains(i)){
                candidates = process(oriFragments.get(i), mutFragments.get(i), "");
            }else{
                candidates.add(mutFragments.get(i));
            }
            if(building.isEmpty()) {
                for (Node candidate : candidates) {
                    List<Vdf> tmp = new ArrayList<>();
                    tmp.add((Vdf) candidate);
                    building.add(tmp);
                }
            }else{
                Queue<List<Vdf>> pres = new LinkedList<>();
                pres.addAll(building);
                building.clear();
                while(!pres.isEmpty()){
                    List<Vdf> pre = pres.poll();
                    for(Node candidate: candidates){
                        List<Vdf> tmp = new ArrayList<>(pre);
                        tmp.add((Vdf) candidate);
                        building.add(tmp);
                    }
                }
            }
        }
        for(List<Vdf> vdfs: building){
            VarDeclarationExpr tmp = new VarDeclarationExpr(mut);
            tmp.addTrace("UPDATEVarDeclarationExpr");
            for(Vdf vdf: vdfs){
                tmp.addAllTrace(vdf.getTrace());
            }
            tmp.setVarDeclFrags(vdfs);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }

    /**
     * Check Vdf's expression part.
     * @param ori: original node
     * @param mut: patch's node
     * @return: list of mutated nodes
     */
    public List<Node> check(Vdf ori, Vdf mut){
        Expr mutExpr = mut.getExpression();
        List<Node> exprCandidates = _checkParser.process(mutExpr, "");
        List<Node> ret = new ArrayList<>();
        ret.add(mut);
        for(Node exprCandidate: exprCandidates){
            Vdf tmp = new Vdf(mut);
            tmp.addTrace("UPDATEVdf");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(mut, ret);
        return ret;
    }


    /**
     * If expr change from MethodInv to InfixExpr, we need to check each expr of InfixExpr.
     * @param ori: original node
     * @param mut: patch's node
     * @param type: mutation type
     * @return: list of mutated nodes
     */
    public List<Node> process(Node ori, Node mut, String type){

//        LevelLogger.debug("UpdateChecker process Called:"+ori + ";" + mut);
        if(ori == null || mut == null){
            return new ArrayList<>();
        }
        if(ori instanceof  AssertStmt && mut instanceof AssertStmt){
            return check((AssertStmt) ori, (AssertStmt) mut, type);
        }else if(ori instanceof Blk && mut instanceof Blk){
            return check((Blk) ori, (Blk) mut);
        }else if(ori instanceof BreakStmt && mut instanceof BreakStmt) {
            return check((BreakStmt) ori, (BreakStmt) mut);
        }else if(ori instanceof CatClause && mut instanceof CatClause) {
            return check((CatClause) ori, (CatClause) mut);
        }else if(ori instanceof ConstructorInv && mut instanceof ConstructorInv) {
            return check((ConstructorInv) ori, (ConstructorInv) mut);
        }else if(ori instanceof ContinueStmt && mut instanceof ContinueStmt) {
            return check((ContinueStmt) ori, (ContinueStmt) mut);
        }else if(ori instanceof DoStmt && mut instanceof DoStmt) {
            return check((DoStmt) ori, (DoStmt) mut);
        }else if(ori instanceof ElseStmt && mut instanceof ElseStmt) {
            return check((ElseStmt) ori, (ElseStmt) mut);
        }else if(ori instanceof EmptyStmt && mut instanceof EmptyStmt) {
            return check((EmptyStmt) ori, (EmptyStmt) mut);
        }else if(ori instanceof EnhancedForStmt && mut instanceof EnhancedForStmt) {
            return check((EnhancedForStmt) ori, (EnhancedForStmt) mut);
        }else if(ori instanceof ExpressionStmt && mut instanceof ExpressionStmt) {
            return check((ExpressionStmt) ori, (ExpressionStmt) mut, type);
        }else if(ori instanceof ForStmt && mut instanceof ForStmt) {
            return check((ForStmt) ori, (ForStmt) mut);
        }else if(ori instanceof IfStmt && mut instanceof IfStmt) {
            return check((IfStmt) ori, (IfStmt) mut);
        }else if(ori instanceof LabeledStmt && mut instanceof LabeledStmt) {
            return check((LabeledStmt) ori, (LabeledStmt) mut);
        }else if(ori instanceof ReturnStmt && mut instanceof ReturnStmt) {
            return check((ReturnStmt) ori, (ReturnStmt) mut);
        }else if(ori instanceof SuperConstructorInv && mut instanceof SuperConstructorInv) {
            return check((SuperConstructorInv) ori, (SuperConstructorInv) mut);
        }else if(ori instanceof SwCase && mut instanceof SwCase) {
            return check((SwCase) ori, (SwCase) mut);
        }else if(ori instanceof SwitchStmt && mut instanceof SwitchStmt) {
            return check((SwitchStmt) ori, (SwitchStmt) mut);
        }else if(ori instanceof SynchronizedStmt && mut instanceof SynchronizedStmt) {
            return check((SynchronizedStmt) ori, (SynchronizedStmt) mut);
        }else if(ori instanceof ThrowStmt && mut instanceof ThrowStmt) {
            return check((ThrowStmt) ori, (ThrowStmt) mut);
        }else if(ori instanceof TryStmt && mut instanceof TryStmt) {
            return check((TryStmt) ori, (TryStmt) mut);
        }else if(ori instanceof TypeDeclarationStmt && mut instanceof TypeDeclarationStmt) {
            return check((TypeDeclarationStmt) ori, (TypeDeclarationStmt) mut);
        }else if(ori instanceof VarDeclarationStmt && mut instanceof VarDeclarationStmt) {
            return check((VarDeclarationStmt) ori, (VarDeclarationStmt) mut);
        }else if(ori instanceof WhileStmt && mut instanceof WhileStmt) {
            return check((WhileStmt) ori, (WhileStmt) mut);
        }else if(ori instanceof AryAcc && mut instanceof AryAcc) {
            return check((AryAcc) ori, (AryAcc) mut);
        }else if(ori instanceof AryCreation && mut instanceof AryCreation) {
            return check((AryCreation) ori, (AryCreation) mut);
        }else if(ori instanceof AryInitializer && mut instanceof AryInitializer) {
            return check((AryInitializer) ori, (AryInitializer) mut);
        }else if(ori instanceof Assign && mut instanceof Assign) {
            return check((Assign) ori, (Assign) mut);
        }else if(ori instanceof AssignOperator && mut instanceof AssignOperator) {
            return check((AssignOperator) ori, (AssignOperator) mut);
        }else if(ori instanceof BoolLiteral && mut instanceof BoolLiteral) {
            return check((BoolLiteral) ori, (BoolLiteral) mut);
        }else if(ori instanceof CastExpr && mut instanceof CastExpr) {
            return check((CastExpr) ori, (CastExpr) mut);
        }else if(ori instanceof CharLiteral && mut instanceof CharLiteral) {
            return check((CharLiteral) ori, (CharLiteral) mut);
        }else if(ori instanceof ClassInstCreation && mut instanceof ClassInstCreation) {
            return check((ClassInstCreation) ori, (ClassInstCreation) mut);
        }else if(ori instanceof ConditionalExpr && mut instanceof ConditionalExpr) {
            return check((ConditionalExpr) ori, (ConditionalExpr) mut);
        }else if(ori instanceof CreationRef && mut instanceof CreationRef) {
            return check((CreationRef) ori, (CreationRef) mut);
        }else if(ori instanceof DoubleLiteral && mut instanceof DoubleLiteral) {
            return check((DoubleLiteral) ori, (DoubleLiteral) mut);
        }else if(ori instanceof ExpressionMethodRef && mut instanceof ExpressionMethodRef) {
            return check((ExpressionMethodRef) ori, (ExpressionMethodRef) mut);
        }else if(ori instanceof FieldAcc && mut instanceof FieldAcc) {
            return check((FieldAcc) ori, (FieldAcc) mut, type);
        }else if(ori instanceof FloatLiteral && mut instanceof FloatLiteral) {
            return check((FloatLiteral) ori, (FloatLiteral) mut);
        }else if(ori instanceof InfixExpr && mut instanceof InfixExpr) {
            return check((InfixExpr) ori, (InfixExpr) mut, type);
        }else if(ori instanceof InfixOperator && mut instanceof InfixOperator) {
            return check((InfixOperator) ori, (InfixOperator) mut);
        }else if(ori instanceof InstanceofExpr && mut instanceof InstanceofExpr) {
            return check((InstanceofExpr) ori, (InstanceofExpr) mut);
        }else if(ori instanceof IntLiteral && mut instanceof IntLiteral) {
            return check((IntLiteral) ori, (IntLiteral) mut);
        }else if(ori instanceof LambdaExpr && mut instanceof LambdaExpr) {
            return check((LambdaExpr) ori, (LambdaExpr) mut);
        }else if(ori instanceof LongLiteral && mut instanceof LongLiteral) {
            return check((LongLiteral) ori, (LongLiteral) mut);
        }else if(ori instanceof MethodInv && mut instanceof MethodInv) {
            System.err.println("MethodInv Called!!!");
            return check((MethodInv) ori, (MethodInv) mut, type);
        }else if(ori instanceof MethodRef && mut instanceof MethodRef) {
            return check((MethodRef) ori, (MethodRef) mut);
        }else if(ori instanceof MType && mut instanceof MType) {
            return check((MType) ori, (MType) mut);
        }else if(ori instanceof NillLiteral && mut instanceof NillLiteral) {
            return check((NillLiteral) ori, (NillLiteral) mut);
        }else if(ori instanceof NumLiteral && mut instanceof NumLiteral) {
            return check((NumLiteral) ori, (NumLiteral) mut);
        }else if(ori instanceof ParenthesiszedExpr && mut instanceof ParenthesiszedExpr) {
            return check((ParenthesiszedExpr) ori, (ParenthesiszedExpr) mut, type);
        }else if(ori instanceof PostfixExpr && mut instanceof PostfixExpr) {
            return check((PostfixExpr) ori, (PostfixExpr) mut, type);
        }else if(ori instanceof PostOperator && mut instanceof PostOperator) {
            return check((PostOperator) ori, (PostOperator) mut);
        }else if(ori instanceof PrefixExpr && mut instanceof PrefixExpr) {
            return check((PrefixExpr) ori, (PrefixExpr) mut, type);
        }else if(ori instanceof PrefixOperator && mut instanceof PrefixOperator) {
            return check((PrefixOperator) ori, (PrefixOperator) mut);
        }else if(ori instanceof QName && mut instanceof QName) {
            return check((QName) ori, (QName) mut, type);
        }else if(ori instanceof SName && mut instanceof SName) {
            return check((SName) ori, (SName) mut, type);
        }else if(ori instanceof StrLiteral && mut instanceof StrLiteral) {
            return check((StrLiteral) ori, (StrLiteral) mut);
        }else if(ori instanceof SuperFieldAcc && mut instanceof SuperFieldAcc) {
            return check((SuperFieldAcc) ori, (SuperFieldAcc) mut);
        }else if(ori instanceof SuperMethodInv && mut instanceof SuperMethodInv) {
            return check((SuperMethodInv) ori, (SuperMethodInv) mut);
        }else if(ori instanceof SuperMethodRef && mut instanceof SuperMethodRef) {
            return check((SuperMethodRef) ori, (SuperMethodRef) mut);
        }else if(ori instanceof Svd && mut instanceof Svd) {
            return check((Svd) ori, (Svd) mut);
        }else if(ori instanceof ThisExpr && mut instanceof ThisExpr) {
            return check((ThisExpr) ori, (ThisExpr) mut);
        }else if(ori instanceof TyLiteral && mut instanceof TyLiteral) {
            return check((TyLiteral) ori, (TyLiteral) mut);
        }else if(ori instanceof TypeMethodRef && mut instanceof TypeMethodRef) {
            return check((TypeMethodRef) ori, (TypeMethodRef) mut);
        }else if(ori instanceof VarDeclarationExpr && mut instanceof VarDeclarationExpr) {
            return check((VarDeclarationExpr) ori, (VarDeclarationExpr) mut);
        }else if(ori instanceof Vdf && mut instanceof Vdf) {
            return check((Vdf) ori, (Vdf) mut);
        }
        // unexpected situation
        else{
            List<Node> ret = new ArrayList<>();
            ret.add(mut);
            return ret;
        }
    }


}
