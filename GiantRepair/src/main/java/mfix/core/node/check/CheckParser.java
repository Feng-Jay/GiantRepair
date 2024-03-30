package mfix.core.node.check;


import mfix.common.conf.Constant;
import mfix.common.java.Subject;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.*;
import mfix.core.node.ast.stmt.*;
import mfix.core.node.match.metric.LevenShteinDistance;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.*;

/*
This class used to parse stmt and check whether each statement's variables or methodCalls are valid.
 */
public class CheckParser {

    private final Scope _localScope; // _localScope means the whole file's scope.
    private final Scope _funcScope; // _funcScope means the scopes under function
    private final Map<String, Scope> _name2Scope;

    private final List<Scope> _importScopes; // _importScopes contains all import file's scope

    // Max candidates number of Statement node.
    private final int MAXSTMTRETURNSIZE = 500;

    // Max candidates number of Expression node.
    private final int MAXSUBMEMBERSIZE = 500;

    private final boolean _combineOption = Constant.COMBINATION_OPTION;

    private Subject _subject;

    private Tracer _trace;

    public CheckParser(Scope scope, Scope funcScope, List<Scope> importScopes, Subject subject){
        _localScope = scope;
        _funcScope = funcScope;
        _importScopes = importScopes;
        _name2Scope = new HashMap<>();
        _subject = subject;
        _trace = new Tracer();
        for(Scope scope1: _importScopes){
            _name2Scope.put(scope1.getClassName(), scope1);
        }
        // due to local scope can have multi classs under same file, use a for loop
        for(Scope scope1: _localScope.getChildScopes()){
            _name2Scope.put(scope1.getClassName(), scope1);
        }
    }

    /**
     * judge whether a type is Iterable or not.
     * @param type: type
     * @return: true if Iterable, false if not.
     */
    public Boolean isIterableType(String type){
        return type.startsWith("List") || type.startsWith("Queue")
                || type.startsWith("Set") || type.startsWith("Map");
    }

    /**
     * Get all Method members of java's Iterable type.
     * @param retType: required return type
     * @return: list of method members
     * TODO: add more Iterable types.
     */
    public List<String> getIterableMethodMember(String retType){
        Map<String, List<String>> retType2Methods = new HashMap<>();
        List<String> booleanMehtod = new ArrayList<>();
        booleanMehtod.add("isEmpty");
        retType2Methods.put("Boolean", booleanMehtod);

        if(retType.isEmpty()){
            return booleanMehtod;
        }
        if(retType2Methods.containsKey(retType)) return retType2Methods.get(retType);
        else return new ArrayList<>();
    }

    /**
     * Unify all the types to same format.
     * @param type: type
     * @return: unified type
     */
    public String transTypes(String type){
        switch (type){
            case "byte": return "Byte";
            case "short": return "Short";
            case "char": return "Character";
            case "int": return "Integer";
            case "long": return "Long";
            case "float": return "Float";
            case "double": return "Double";
            case "boolean": return "Boolean";
            default: return type;
        }
    }

    /**
     * Convert big types to small types.
     * @param type: type
     * @return: small type
     */
    public String transTypesVerse(String type){
        switch (type){
            case "Byte": return "byte";
            case "Short": return "short";
            case "Character": return "char";
            case "Integer": return "int";
            case "Long": return "long";
            case "Float": return "float";
            case "Double": return "double";
            case "Boolean": return "boolean";
            default: return type;
        }
    }

    /**
     * Get compatible table for primitive types.
     * @return: compatible table
     */
    public Map<String, List<String>> getCompatibleTable(){
        Map<String, List<String>> ret = new HashMap<>();
        String[] keysArray = {"Byte", "Short", "Character", "Integer", "Long", "Float", "Double", "Boolean"};
        for(String key: keysArray){
            switch (key){
                case "Byte":
                    List<String> values = new ArrayList<>(Arrays.asList("Byte", "Short", "Integer", "Long", "Float", "Double"));
                    ret.put(key, values);
                    break;
                case "Short":
                    List<String> values1 = new ArrayList<>(Arrays.asList("Short", "Integer", "Long", "Float", "Double"));
                    ret.put(key, values1);
                    break;
                case "Character":
                    List<String> values2 = new ArrayList<>(Arrays.asList("Character", "Integer", "Long", "Float", "Double"));
                    ret.put(key, values2);
                    break;
                case "Integer":
                    List<String> values3 = new ArrayList<>(Arrays.asList("Integer", "Long", "Float", "Double"));
                    ret.put(key, values3);
                    break;
                case "Long":
                    List<String> values4 = new ArrayList<>(Arrays.asList("Long", "Float", "Double"));
                    ret.put(key, values4);
                    break;
                case "Float":
                    List<String> values5 = new ArrayList<>(Arrays.asList("Float", "Double"));
                    ret.put(key, values5);
                    break;
                case "Double":
                    List<String> values6 = new ArrayList<>(List.of("Double"));
                    ret.put(key, values6);
                    break;
                case "Boolean":
                    List<String> values7 = new ArrayList<>(List.of("Boolean"));
                    ret.put(key, values7);
                    break;
            }
        }
        return ret;
    }

    /**
     * If types are both primitive, judge if they are compatible or not.
     * If there is one primitive, one the selfDefined, return false.
     * If both are selfDefined, judge if their type is same or not
     * java's primitive types are: byte, short, char, int, long, float, double and boolean
     * @param type1: type1
     * @param type2: type2
     * @return: true if compatible, false if not.
     */
    public boolean typesAreCompatible(String type1, String type2) {
        // first convert primitive type to wrapper type
        if(type1.isEmpty() || type2.isEmpty())
            return true;
        Map<String, List<String>> compatibleTable = getCompatibleTable();

        type1 = transTypes(type1);
        type2 = transTypes(type2);

        if (compatibleTable.containsKey(type1) && compatibleTable.get(type1).contains(type2)) {
            return compatibleTable.get(type1).contains(type2);
        }

        return type1.equals(type2);
    }

    public Queue<List<Stmt>> combineAllStmt(List<Stmt> nodes){
        if(nodes == null || nodes.isEmpty()){
            return new LinkedList<>();
        }
        if(_combineOption){
            int counter = 1;
            Queue<List<Stmt>> building = new LinkedList<>();
            for(Stmt node: nodes){
                List<Node> currentCandidates = process(node, "");
                counter *= currentCandidates.size();

                List<List<Stmt>> pres = new ArrayList<>(building);
                building.clear();

                if(pres.isEmpty()){
                    for(Node candidate: currentCandidates){
                        List<Stmt> buildTmp = new ArrayList<>(); buildTmp.add((Stmt) candidate);
                        building.add(buildTmp);
                    }
                }else{
                    for(List<Stmt> pre: pres){
                        for(Node candidate: currentCandidates){
                            List<Stmt> buildTmp = new ArrayList<>(pre);
                            buildTmp.add((Stmt) candidate);
                            building.add(buildTmp);
                        }
                    }
                }
            }
            assert building.size() == counter;
            return building;
        }else {
            Queue<List<Stmt>> ret = new LinkedList<>();
            for(Stmt node: nodes){
                List<Node> currentCandidates = process(node, "");
                for(Node candidate: currentCandidates){
                    List<Stmt> buildTmp = new ArrayList<>(nodes);
                    buildTmp.set(nodes.indexOf(node), (Stmt) candidate);
                    ret.add(buildTmp);
                }
            }
            return ret;
        }
    }

    public Queue<List<CatClause>> combineAllCatch(List<CatClause> nodes){
        if(nodes == null || nodes.isEmpty()){
            return new LinkedList<>();
        }
        if(_combineOption){
            int counter = 1;
            Queue<List<CatClause>> building = new LinkedList<>();
            for(CatClause node: nodes){
                List<Node> currentCandidates = process(node, "");
                counter *= currentCandidates.size();
                List<List<CatClause>> pres = new ArrayList<>(building);
                building.clear();
                if(pres.isEmpty()){
                    for(Node candidate: currentCandidates){
                        List<CatClause> buildTmp = new ArrayList<>(); buildTmp.add((CatClause) candidate);
                        building.add(buildTmp);
                    }
                }else{
                    for(List<CatClause> pre: pres){
                        for(Node candidate: currentCandidates){
                            List<CatClause> buildTmp = new ArrayList<>(pre);
                            buildTmp.add((CatClause) candidate);
                            building.add(buildTmp);
                        }
                    }
                }
            }
            assert building.size() == counter;
            return building;
        }else{
            Queue<List<CatClause>> ret = new LinkedList<>();
            for(CatClause node: nodes){
                List<Node> currentCandidates = process(node, "");
                for(Node candidate: currentCandidates){
                    List<CatClause> buildTmp = new ArrayList<>(nodes);
                    buildTmp.set(nodes.indexOf(node), (CatClause) candidate);
                    ret.add(buildTmp);
                }
            }
            return ret;
        }
    }

    public Queue<List<Vdf>> combineAllVdf(List<Vdf> nodes, String type){
        if(nodes == null || nodes.isEmpty()){
            return new LinkedList<>();
        }
        if(_combineOption){
            int counter = 1;
            Queue<List<Vdf>> building = new LinkedList<>();
            for(Vdf node: nodes){
                List<Node> currentCandidates = process(node, type);
                counter *= currentCandidates.size();
                List<List<Vdf>> pres = new ArrayList<>(building);
                building.clear();
                if(pres.isEmpty()){
                    for(Node candidate: currentCandidates){
                        List<Vdf> buildTmp = new ArrayList<>(); buildTmp.add((Vdf) candidate);
                        building.add(buildTmp);
                    }
                }else{
                    for(List<Vdf> pre: pres){
                        for(Node candidate: currentCandidates){
                            List<Vdf> buildTmp = new ArrayList<>(pre);
                            buildTmp.add((Vdf) candidate);
                            building.add(buildTmp);
                        }
                    }
                }
            }
            assert building.size() == counter;
            return building;
        }else{
            Queue<List<Vdf>> ret = new LinkedList<>();
            for(Vdf node: nodes){
                List<Node> currentCandidates = process(node, type);
                for(Node candidate: currentCandidates){
                    List<Vdf> buildTmp = new ArrayList<>(nodes);
                    buildTmp.set(nodes.indexOf(node), (Vdf) candidate);
                    ret.add(buildTmp);
                }
            }
            return ret;
        }
    }


    public Queue<List<Expr>> combineAllExpr(List<Expr> nodes, List<String> types){
        if(nodes == null || nodes.isEmpty()){
            return new LinkedList<>();
        }
        if(_combineOption){
            int counter = 1;
            Queue<List<Expr>> building = new LinkedList<>();
            for(Expr node: nodes){
                List<Node> currentCandidates = process(node, types.get(nodes.indexOf(node)));
                counter *= currentCandidates.size();
                List<List<Expr>> pres = new ArrayList<>(building);
                building.clear();
                if(pres.isEmpty()){
                    for(Node candidate: currentCandidates){
                        List<Expr> buildTmp = new ArrayList<>(); buildTmp.add((Expr) candidate);
                        building.add(buildTmp);
                    }
                }else{
                    for(List<Expr> pre: pres){
                        for(Node candidate: currentCandidates){
                            List<Expr> buildTmp = new ArrayList<>(pre);
                            buildTmp.add((Expr) candidate);
                            building.add(buildTmp);
                        }
                    }
                }
            }
            assert building.size() == counter;
            return building;
        }else{
            Queue<List<Expr>> ret = new LinkedList<>();
            for(Expr node: nodes){
                List<Node> currentCandidates = process(node, types.get(nodes.indexOf(node)));
                for(Node candidate: currentCandidates){
                    List<Expr> buildTmp = new ArrayList<>(nodes);
                    buildTmp.set(nodes.indexOf(node), (Expr) candidate);
                    ret.add(buildTmp);
                }
            }
            return ret;
        }
    }

    /**
     * Get the SName node's type from local file, usually a variable.
     * @param varName: variable name
     * @param lineNum: variable's line number
     * @return: type of variable
     */
    public String getVarType(String varName, int lineNum){
        // first: searching in function scopes
        Scope scopePointer = _funcScope;
        Queue<Scope> children = new LinkedList<>();
        children.add(scopePointer);
        while(!children.isEmpty()){
            scopePointer = children.poll();
            if(lineNum >= scopePointer.getStartLine() && lineNum <= scopePointer.getEndLine()){
                if(scopePointer.containsVar(varName) != null){
                    return scopePointer.containsVar(varName).getType();
                }
                children.addAll(scopePointer.getChildScopes());
            }
        }
        scopePointer = _localScope;
        children.add(scopePointer);
        while (!children.isEmpty()){
            scopePointer = children.poll();
            if(scopePointer.containsVar(varName) != null){
                return scopePointer.containsVar(varName).getType();
            }
            children.addAll(scopePointer.getChildScopes());
        }
        return "";
    }

    /**
     * Given function name and its arguments, get its return type.
     * @param funcName: function name
     * @param amount: arguments
     * @param lineNumber: line number
     * @return: return type
     */
    public String getFuncRetType(String funcName, int amount, int lineNumber){
        Scope scopePointer = _localScope;
        // first search in local scope to get return type.
        List<FuncSignature> funcs = new ArrayList<>();
        Queue<Scope> candidateScopes = new LinkedList<>();
        candidateScopes.add(scopePointer);
        while(!candidateScopes.isEmpty()){
            scopePointer = candidateScopes.poll();
            funcs = scopePointer.getFuncs();
            for(FuncSignature func: funcs){
                if(func.getFuncName().equals(funcName) && func.getParameters().size() == amount){
                    return func.getRetType();
                }
            }
            if(!scopePointer.getChildScopes().isEmpty()){
                candidateScopes.addAll(scopePointer.getChildScopes());
            }
        }
        // if nothing find in local scope, search in import scope.
        for(Scope importScope: _importScopes){
            funcs = importScope.getFuncs();
            for(FuncSignature func: funcs){
                if(func.getFuncName().equals(funcName) && func.getParameters().size() == amount){
                    return func.getRetType();
                }
            }
        }
        if(funcName.equals("max")|| funcName.equals("min") || funcName.equals("size")){
            return "Integer";
        }
        return "";
    }

    /**
     * An Iterator Interface, call getType method to get the type of each node.
     * @param exprs: expressions
     * @return: list of types
     */
    public List<String> getTypes(List<Expr> exprs){
        List<String> ret = new ArrayList<>();
        for(Expr expr: exprs){
            ret.add(getType(expr));
        }
        return ret;
    }

    /**
     * get Expression's type. for now just support SNAME & METHODINVOCATION & FieldACCESS & QNAME
     * @param expr: expression
     * @return: type of expression
     */
    public String getType(Expr expr){
        if(expr instanceof SName){
            return getVarType(((SName) expr).getName(), expr.getStartLine());
        }else if(expr instanceof QName){
            // in jdt, a QualifiedName represents references to types
            // (classes, interfaces, enums, etc.), packages, or static fields/methods from other classes.
            return parseQName(expr.toString());
        }else if(expr instanceof FieldAcc){
            // TOADD: MethodCall().id situation can not processed by this method
            return parseFieldAccess(expr, _localScope);
        }else if(expr instanceof NumLiteral){
            String value = expr.toSrcString().toString();
            if(value.contains(".")) {
                return "Double";
            }else{
                return "Integer";
            }
        }else if(expr instanceof MethodInv){
            return getFuncRetType(((MethodInv) expr).getName().toString(), ((MethodInv)expr).getArguments().getExpr().size(), expr.getStartLine());
        }else if(expr instanceof BoolLiteral){
            return "Boolean";
        }
        return "";
    }

    /**
     * A QName node is consist of : Name. SimpleName
     * @param QName: QName node
     * @return: type of QName
     */
    public String parseQName(String QName){
        String[] fieldsTmp = QName.split("\\.");
        List<String> fields = Arrays.asList(fieldsTmp);
        List<String> allImports = new ArrayList<>();
        for(Scope importScope: _importScopes){
            allImports.add(importScope.getClassName());
        }
        Scope currentScope = _localScope;
        String retType = "";
        for(String field: fields){
            if(allImports.contains(field)){
                currentScope = _name2Scope.get(field);
                retType = field;
            }else if(currentScope.containsStaticVar(field)!=null) {
                retType = currentScope.containsStaticVar(field).getType();
                currentScope = _name2Scope.get(retType);
                if(fields.indexOf(field)==fields.size()-1) return retType;
                else if(currentScope == null) return "";
            }else{
                return  "";
            }
        }
        return retType;
    }

    /**
     * Parse FieldAccess node
     * @param fieldAccess: FieldAccess node
     * @param currentScope: current scope
     * @return: type of fieldAccess
     */
    public String parseFieldAccess(Expr fieldAccess, Scope currentScope){
        Expr exprPart = ((FieldAcc) fieldAccess).getExpression();
        Expr idPart = ((FieldAcc) fieldAccess).getIdentifier();
        String scopeName = "";
        if(exprPart instanceof FieldAcc){
            scopeName = parseFieldAccess(exprPart, currentScope);
        }else if(exprPart instanceof SName){
            scopeName = getVarType(((SName) exprPart).getName(), exprPart.getStartLine());
        }else if(exprPart instanceof QName){
            scopeName = parseQName(exprPart.toString());
        }else if(exprPart instanceof ThisExpr) {
            scopeName = currentScope.getClassName();
        }else if(exprPart instanceof MethodInv){
            MethodInv tmpExpr = new MethodInv((MethodInv) exprPart);
            scopeName = getFuncRetType(tmpExpr.getName().toString(), tmpExpr.getArguments().getExpr().size(), exprPart.getStartLine());
        }else{
            scopeName = "";
        }
        if(_name2Scope.containsKey(scopeName)){
            currentScope = _name2Scope.get(scopeName);
            for(Variable var: currentScope.getVars()){
                if(var.getName().equals(idPart.toString())){
                    return var.getType();
                }
            }
        }
        return "";
    }

    public List<FuncSignature> getUsableFuncs(){
        Scope scopePointer = _localScope;
        Queue<Scope> children = new LinkedList<>();
        children.add(scopePointer);
        while(!children.isEmpty()){
            scopePointer = children.poll();
            if(scopePointer.getClassName().equals(_funcScope.getClassName()) && scopePointer.getStartLine() == _subject.getFuncBeginLine()){
                LevelLogger.debug("Find corresponding function!!!");
                break;
            }
            children.addAll(scopePointer.getChildScopes());
        }
        scopePointer = scopePointer.getParent();
        List<FuncSignature> ret = new ArrayList<>();
        while(scopePointer != null){
            ret.addAll(scopePointer.getFuncs());
            scopePointer = scopePointer.getParent();
        }
        return ret;
    }

    /**
     * get this file's accessible variables this method can use.
     * @param lineNum: line number
     * @return: list of variables
     */
    public List<Variable> getUsableVars(int lineNum){
        Scope scopePointer = _funcScope;
        Map<String, String> type2SuperType = _localScope.getType2SuperClass();
        Queue<Scope> children = new LinkedList<>();
        List<Variable> ret = new ArrayList<>();
        children.add(scopePointer);
        while (!children.isEmpty()){
            scopePointer = children.poll();
            if(lineNum >= scopePointer.getStartLine() && lineNum <= scopePointer.getEndLine()){
                ret.addAll(scopePointer.getVars());
                children.addAll(scopePointer.getChildScopes());
            }
        }
        // search method in _local scope, add all usable vars to ret.
        scopePointer = _localScope;
        children.add(scopePointer);
        while(!children.isEmpty()){
            scopePointer = children.poll();
            if(scopePointer.getClassName().equals(_funcScope.getClassName()) && scopePointer.getStartLine() == _subject.getFuncBeginLine()){
                LevelLogger.debug("Find corresponding function!!!");
               break;
            }
            children.addAll(scopePointer.getChildScopes());
        }
        scopePointer = scopePointer.getParent();
        while(scopePointer != null){
            ret.addAll(scopePointer.getVars());
            scopePointer = scopePointer.getParent();
        }
        List<Variable> retNew = new ArrayList<>(ret);
        for(Variable var: ret){
            if(type2SuperType.containsKey(var.getType())){
                Variable newVar = new Variable(var, type2SuperType.get(var.getType()));
                retNew.add(newVar);
            }
        }
        return retNew;
    }

    /**
     * Compute candidates' similarity with mut node and rank/filter them.
     * @param mut: mutation node.
     * @param candidates: candidates node.
     * @return: ranked and filtered candidates.
     */
    public List<Node> rankAndFilterBySimilarityStmt(Node mut, List<Node> candidates){
        String mutString = mut.toString();
        int MaxCandidatesLength = 100000;
        if (candidates.size() > MaxCandidatesLength){
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


    /*****************************************Following Are Statement Check Methods********************************************/

    /**
     * Get all possible mutations of this node.
     * @param node: node
     * @return: list of this node's mutations.
     */
    public List<Node> check(AssertStmt node){
        ParenthesiszedExpr expr = (ParenthesiszedExpr) node.getExpression();
        List<Node> ret = new ArrayList<>();
//        node.addTrace("AssertStmt");
        ret.add(node);
        for(Node node1: process(expr, "Boolean")){
            AssertStmt tmp = new AssertStmt(node);
            tmp.setExpression((Expr)node1);
            ret.add(tmp);
            tmp.addTrace("AssertStmt");
            tmp.addAllTrace(node1.getTrace());
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: break stmt
     * @return: list of this node.
     */
    public List<Node> check(BreakStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("BreakStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * For block statement, try to mutate all statements in it.
     * @param node: block statement
     * @return: list of this node's mutations.
     */
    public List<Node> check(Blk node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("Blk");
        ret.add(node);
        List<Stmt> blockStmts = node.getStatement();
        // if statements amount > 2, skip and do not mutate.
        if(blockStmts.size() > 2){
            return ret;
        }
        Queue<List<Stmt>> building = combineAllStmt(node.getStatement());
        while(!building.isEmpty()){
            List<Stmt> stmts = building.poll();
            Blk tmpBlk = new Blk(node);
            tmpBlk.setStatement(stmts);
            tmpBlk.addTrace("Blk");
            for (Stmt stmt : stmts) {
                tmpBlk.addAllTrace(stmt.getTrace());
            }
            ret.add(tmpBlk);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    public List<Node> selectConstructorInv(ConstructorInv node){
        List<ConstructorNode> constructorNodes = _localScope.getConstructors();
        Map<String, List<String>> candidatesConstatnt = new HashMap<>();
        List<String> ints = new ArrayList<>();
        ints.add("0"); ints.add("-1"); ints.add("1");
        List<String> booleans = new ArrayList<>();
        booleans.add("false"); booleans.add("true");
        candidatesConstatnt.put("Integer", ints);
        candidatesConstatnt.put("int", ints);
        candidatesConstatnt.put("Boolean", booleans);
        candidatesConstatnt.put("boolean", booleans);
        List<Node> ret = new ArrayList<>();
        ret.add(node);

        for(ConstructorNode constructorNode: constructorNodes){
            List<Variable> parameters = constructorNode.getParameterList();
            if(Math.abs(parameters.size() - node.getArguments().getExpr().size()) > 1){
                continue;
            }
            Queue<List<Variable>> parameterCombination = new LinkedList<>();
            Queue<List<Variable>> parameterCombinationTmp = new LinkedList<>();
            for(Variable var: parameters){
                if(!candidatesConstatnt.containsKey(var.getType())){
                    break;
                }
                if(parameterCombination.isEmpty()){
                    for(String constant: candidatesConstatnt.get(var.getType())){
                        List<Variable> tmp = new ArrayList<>();
                        tmp.add(new Variable(var.getLine(), constant, var.getType(), var.getNode()));
                        parameterCombination.add(tmp);
                    }
                    continue;
                }
                while(!parameterCombination.isEmpty()){
                    List<Variable> tmp = parameterCombination.poll();
                    for(String constant: candidatesConstatnt.get(var.getType())){
                        List<Variable> tmp2 = new ArrayList<>(tmp);
                        tmp2.add(new Variable(var.getLine(), constant, var.getType(), var.getNode()));
                        parameterCombinationTmp.add(tmp2);
                    }
                }
                parameterCombination.addAll(parameterCombinationTmp);
                parameterCombinationTmp.clear();
            }
            // if parameter amount is not equal, skip this constructor.
            if(parameterCombination.isEmpty() || parameters.isEmpty()){
                List<Variable> tmp = new ArrayList<>();
                Variable var = new Variable(0, "", "", node);
                tmp.add(var);
                parameterCombination.add(tmp);
            }
            if((!parameters.isEmpty() && parameterCombination.peek() == null) || (!parameters.isEmpty() && parameterCombination.peek().size() != parameters.size())){
                continue;
            }
            for(List<Variable> parameter: parameterCombination){
                ConstructorInv tmp = new ConstructorInv(node);
                ExprList exprList = new ExprList(node.getArguments());
                List<Expr> exprs = new ArrayList<>();
                for(Variable var: parameter){
                    SName tmpExpr = new SName(node.getArguments().getFileName(), node.getArguments().getStartLine(), node.getArguments().getEndLine(), node.getArguments().getOriNode());
                    tmpExpr.setName(var.getName());
                    exprs.add(tmpExpr);
                }
                exprList.setExprs(exprs);
                tmp.setArguments(exprList);
                tmp.addTrace("ConstructorInv");
                ret.add(tmp);
            }
        }
        return ret;
    }
    /**
     * this(arguments);
     * for now, no examples need this.
     * @param node: ConstructorInv node
     * @return: list of this node's mutations.
     */
    public List<Node> check(ConstructorInv node){
       List<Node> ret = new ArrayList<>();
       ret.add(node);
       ret.addAll(selectConstructorInv(node));
       ret = rankAndFilterBySimilarityStmt(node, ret);
       return ret;
    }

    /**
     * skip this node
     * @param node: continue stmt
     * @return: list of this node.
     */
    public List<Node> check(ContinueStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ContinueStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Do-while stmt
     * @param node: do-while stmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(DoStmt node){
        Stmt body = node.getBody();
        Expr expr = node.getExpression();

        List<Node> bodyCandidates = process(body, "");
        List<Node> exprCandidates = process(expr, "Boolean");

        List<Node> ret = new ArrayList<>();
//        node.addTrace("DoStmt");
        ret.add(node);
        if(_combineOption){
            for (Node bodyCandidate : bodyCandidates) {
                for (Node exprCandidate : exprCandidates) {
                    DoStmt tmp = new DoStmt(node);
                    tmp.setBody((Stmt) bodyCandidate);
                    tmp.setExpression((Expr) exprCandidate);
                    ret.add(tmp);
                }
            }
        }else{
            for(Node bodyCandidate: bodyCandidates){
                DoStmt tmp = new DoStmt(node);
                tmp.addTrace("DoStmt");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setBody((Stmt) bodyCandidate);
                tmp.setExpression(expr);
                ret.add(tmp);
            }
            for(Node exprCandidate: exprCandidates){
                DoStmt tmp = new DoStmt(node);
                tmp.addTrace("DoStmt");
                tmp.addAllTrace(exprCandidate.getTrace());
                tmp.setBody(body);
                tmp.setExpression((Expr) exprCandidate);
                ret.add(tmp);
            }
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * skip this node
     * @param node: empty stmt
     * @return: list of this node.
     */
    public List<Node> check(EmptyStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("EmptyStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * skip this node, no examples in dataset.
     * @param node: EnhancedForStmt
     * @return: list of this node.
     */
    public List<Node> check(EnhancedForStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("EnhancedForStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * ExpressionStmt control many expressions, so just pass the expr to specific check method.
     * @param node: ExpressionStmt
     * @return: list of this node's muatation.
     */
    public List<Node> check(ExpressionStmt node){
        Expr expr = node.getExpression();
        List<Node> exprCandidates = process(expr, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ExpressionStmt");
        ret.add(node);
        for (Node exprCandidate : exprCandidates) {
            ExpressionStmt tmp = new ExpressionStmt(node);
            tmp.addTrace("ExpressionStmt");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * for now, just mutate the condition expr.
     * @param node: forStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(ForStmt node){
        Expr condition = node.getCondition();
        Stmt body      = node.getBody();
        List<Node> conditionCandidatesTmp = process(condition, "Boolean");
        List<Node> conditionCandidates = new ArrayList<>();
        List<Node> bodyCandidates      = process(body, "");
        if(condition instanceof InfixExpr){
            // for loop: change condition from methodCall to </<= 0,1...
            // StarCoder Math_10
            InfixExpr oriInfixExpr = (InfixExpr) condition;
            if(oriInfixExpr.getOperator().toString().contains("<")){
                List<String> candidateNumLiterals = new ArrayList<>();
                candidateNumLiterals.add("0"); candidateNumLiterals.add("1");
                for(String selectNum: candidateNumLiterals){
                    InfixExpr mutatedInfixExpr = new InfixExpr(oriInfixExpr);
                    mutatedInfixExpr.addTrace("ForCondition2Num");
                    mutatedInfixExpr.setLeftHandSide(oriInfixExpr.getLhs());
                    NumLiteral zeroOrOne = new NumLiteral(oriInfixExpr.getFileName(), oriInfixExpr.getStartLine(), oriInfixExpr.getEndLine(), oriInfixExpr.getOriNode());
                    zeroOrOne.setValue(selectNum);
                    mutatedInfixExpr.setRightHandSide(zeroOrOne);
                    conditionCandidates.add(mutatedInfixExpr);
                }
            }
        }
        conditionCandidates.addAll(conditionCandidatesTmp);
        conditionCandidatesTmp.clear();

        List<Node> ret = new ArrayList<>();
//        node.addTrace("ForStmt");
        ret.add(node);
        if(_combineOption) {
            for (Node conditionCandidate : conditionCandidates) {
                for (Node bodyCandidate : bodyCandidates) {
                    ForStmt tmp = new ForStmt(node);
                    tmp.setCondition((Expr) conditionCandidate);
                    tmp.setBody((Stmt) bodyCandidate);
                    ret.add(tmp);
                }
            }
        }else{
            for (Node conditionCandidate : conditionCandidates) {
                ForStmt tmp = new ForStmt(node);
                tmp.addTrace("ForStmt");
                tmp.addAllTrace(conditionCandidate.getTrace());
                tmp.setCondition((Expr) conditionCandidate);
                tmp.setBody(body);
                ret.add(tmp);
            }
            for (Node bodyCandidate : bodyCandidates) {
                ForStmt tmp = new ForStmt(node);
                tmp.addTrace("ForStmt");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setCondition(condition);
                tmp.setBody((Stmt) bodyCandidate);
                ret.add(tmp);
            }
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Randomly combine the if-condition's boolean sub-expressions.
     * @param expr: if-condition
     * @return: list of this node's mutations.
     */
    public List<Node> randomCombineOfIfCondition(Expr expr){
       if(expr instanceof InfixExpr){
           InfixExpr tmpInfixExpr = (InfixExpr) expr;
           if(!tmpInfixExpr.getOperator().toString().equals("&&") && !tmpInfixExpr.getOperator().toString().equals("||")){
               return new ArrayList<>();
           }
           List<Expr> elems = new ArrayList<>();
           Set<InfixOperator> ops = new HashSet<>();
           InfixExpr infixExpr = (InfixExpr) expr;
           Queue<InfixExpr> candidates = new LinkedList<>();
           candidates.add(infixExpr);
           while(!candidates.isEmpty()){
               InfixExpr tmp = candidates.poll();
               ops.add(tmp.getOperator());

               Expr lhs = tmp.getLhs();
               if(lhs instanceof ParenthesiszedExpr) lhs = ((ParenthesiszedExpr) lhs).getExpression();
               if(lhs instanceof InfixExpr &&(((InfixExpr)lhs).getOperator().toString().equals("&&") || ((InfixExpr)lhs).getOperator().toString().equals("||"))){
                   candidates.add((InfixExpr) lhs);
               }else{
                   elems.add(lhs);
               }

               Expr rhs = tmp.getRhs();
                if(rhs instanceof ParenthesiszedExpr) rhs = ((ParenthesiszedExpr) rhs).getExpression();
               if(rhs instanceof InfixExpr &&(((InfixExpr)rhs).getOperator().toString().equals("&&") || ((InfixExpr)rhs).getOperator().toString().equals("||"))){
                   candidates.add((InfixExpr) rhs);
               }else{
                   elems.add(rhs);
               }
           }
           if(ops.size() > 2){
               return new ArrayList<>();
           }
           LevelLogger.debug("Infix elems: "+elems);
           List<InfixOperator> opsList = new ArrayList<>(ops);
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
                   if(index.get(i) == 1) exprs.add(elems.get(i));
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
                   tmpInfix.setOperator(opsList.get(0));
                   tmp = tmpInfix;
               }
               LevelLogger.debug("Add tmp:"+tmp);
               tmp.addTrace("RandomCombineOfIfCondition");
               ret.add(tmp);
           }
           LevelLogger.debug("RandomCombineOfIfCondition: "+ret);
           return ret;
       }else{
           return new ArrayList<>();
       }
    }

    /**
     * Mutate condition, thenBlock and elseBlock(if exist).
     * @param node: ifStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(IfStmt node){
        Expr expr = node.getCondition();
        Stmt then = node.getThen();
        Stmt elseBlock = node.getElse();

        List<Node> exprCandidates = process(expr, "Boolean");
        exprCandidates.addAll(randomCombineOfIfCondition(expr));
        List<Node> thenCandidates = process(then, "");
        List<Node> elseCandidates = new ArrayList<>();
//        if(elseBlock != null){
//            elseCandidates = process(elseBlock, "");
//        }
        List<Node> ret = new ArrayList<>();
//        node.addTrace("IfStmt");
        ret.add(node);
//        LevelLogger.debug("IFSTMT BEGIN!");
//        exprCandidates = exprCandidates.subList(0, 1);
        if(!_combineOption){
            for (Node exprCandidate : exprCandidates) {
                for (Node thenCandidate : thenCandidates) {
//                    if(ret.size() > 20000) break;
                    if (elseCandidates.size() == 0) {
                        IfStmt tmp = new IfStmt(node);
                        Expr condition = (Expr) exprCandidate;
                        tmp.addAllTrace(condition.getTrace());
                        tmp.addAllTrace(thenCandidate.getTrace());
                        tmp.setCondition(condition);
                        tmp.setThen((Stmt) thenCandidate);
                        tmp.addTrace("IfStmt");
                        tmp.addAllTrace(condition.getTrace());
                        tmp.addAllTrace(thenCandidate.getTrace());
                        ret.add(tmp);
                        // add an ! token before condition
                        IfStmt tmp2 = new IfStmt(node);
                        ParenthesiszedExpr tmpParentExpr = new ParenthesiszedExpr(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                        tmpParentExpr.setExpr(condition);
                        PrefixOperator tmpPrefixOp = new PrefixOperator(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                        tmpPrefixOp.setOperatorStr("!");
                        PrefixExpr tmpPrefix = new PrefixExpr(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                        tmpPrefix.setOperator(tmpPrefixOp);
                        tmpPrefix.setExpression(tmpParentExpr);
                        tmpPrefix.addTrace("NOT IfCondition");
                        tmpPrefix.addAllTrace(condition.getTrace());
                        if(expr instanceof PrefixExpr){
                            tmp2.setCondition(condition);
                        }else{
                            tmp2.setCondition(tmpPrefix);
                        }
                        tmp2.setThen((Stmt) thenCandidate);
                        tmp2.addTrace("IfStmt");
                        tmp2.addAllTrace(tmpPrefix.getTrace());
                        tmp2.addAllTrace(thenCandidate.getTrace());
                        ret.add(tmp2);
                    } else {
                        for (Node elseCandidate : elseCandidates) {
                            IfStmt tmp = new IfStmt(node);
                            Expr condition = (Expr) exprCandidate;
                            tmp.addAllTrace(condition.getTrace());
                            tmp.addAllTrace(thenCandidate.getTrace());
                            tmp.addAllTrace(elseCandidate.getTrace());
                            tmp.setCondition(condition);
                            tmp.setThen((Stmt) thenCandidate);
                            tmp.setElse((Stmt) elseCandidate);
                            tmp.addTrace("IfStmt");
                            tmp.addAllTrace(condition.getTrace());
                            tmp.addAllTrace(thenCandidate.getTrace());
                            tmp.addAllTrace(elseCandidate.getTrace());
                            ret.add(tmp);
                            // add an ! token before condition
                            IfStmt tmp2 = new IfStmt(node);
                            ParenthesiszedExpr tmpParentExpr = new ParenthesiszedExpr(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                            tmpParentExpr.setExpr(condition);
                            PrefixOperator tmpPrefixOp = new PrefixOperator(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                            tmpPrefixOp.setOperatorStr("!");
                            PrefixExpr tmpPrefix = new PrefixExpr(condition.getFileName(), condition.getStartLine(), condition.getEndLine(), condition.getOriNode());
                            tmpPrefix.setOperator(tmpPrefixOp);
                            tmpPrefix.setExpression(tmpParentExpr);
                            tmpPrefix.addTrace("NOT IfCondition");
                            tmpPrefix.addAllTrace(condition.getTrace());

                            tmp2.setCondition(tmpPrefix);
                            tmp2.setThen((Stmt) thenCandidate);
                            tmp2.setElse((Stmt) elseCandidate);
                            tmp2.addTrace("IfStmt");
                            tmp2.addAllTrace(tmpPrefix.getTrace());
                            tmp2.addAllTrace(thenCandidate.getTrace());
                            tmp2.addAllTrace(elseCandidate.getTrace());
                            ret.add(tmp2);
                        }
                    }
                }
            }
        }else{
            for(Node exprCandidate: exprCandidates){
                IfStmt tmp = new IfStmt(node);
                tmp.setCondition((Expr) exprCandidate);
                tmp.setThen(then);
                tmp.setElse(elseBlock);
                ret.add(tmp);
            }
            for(Node thenCandidate: thenCandidates){
                IfStmt tmp = new IfStmt(node);
                tmp.setCondition(expr);
                tmp.setThen((Stmt) thenCandidate);
                tmp.setElse(elseBlock);
                ret.add(tmp);
            }
            for(Node elseCandidate: elseCandidates){
                IfStmt tmp = new IfStmt(node);
                tmp.setCondition(expr);
                tmp.setThen(then);
                tmp.setElse((Stmt) elseCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * In fact Else stmt is as same as a Block.
     * @param node: elseStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(ElseStmt node){
        Stmt body = node.getBody();
        List<Node> bodyCandidates = new ArrayList<>();
        bodyCandidates.add(body);
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ElseStmt");
        ret.add(node);
        for (Node bodyCandidate : bodyCandidates) {
            ElseStmt tmp = new ElseStmt(node);
            tmp.addTrace("ElseStmt");
            tmp.addAllTrace(bodyCandidate.getTrace());
            tmp.setBody((Stmt) bodyCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * skip this node
     * @param node: LabeledStmt
     * @return: list of this node.
     */
    public List<Node> check(LabeledStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("LabeledStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return new ArrayList<>(ret);
    }

    /**
     * Mutate return's expression.
     * @param node: returnStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(ReturnStmt node){
        Expr expr = node.getExpression();
        List<Node> exprCandidates = process(expr, "");
        if(expr instanceof NumLiteral){
            List<String> candidates = new ArrayList<>();
            candidates.add("0"); candidates.add("1"); candidates.add("-1");
            for(String candidate: candidates){
                NumLiteral tmp = new NumLiteral(expr.getFileName(), expr.getStartLine(), expr.getEndLine(), expr.getOriNode());
                tmp.setValue(candidate);
                exprCandidates.add(tmp);
            }
        }
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        if(expr == null){
            return new ArrayList<>(ret);
        }
        for (Node exprCandidate : exprCandidates) {
            ReturnStmt tmp = new ReturnStmt(node);
            tmp.addTrace("ReturnStmt");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: SuperConstructorInv
     * @return: list of this node.
     */
    public List<Node> check(SuperConstructorInv node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("SuperConstructorInv");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate swCase's expression.
     * @param node: SwCase
     * @return: list of this node's mutations.
     */
    public List<Node> check(SwCase node){
        Expr expr = node.getExpression();
        List<Node> ret = new ArrayList<>();
//        node.addTrace("SwCase");
        ret.add(node);
        List<Node> exprCandidates = process(expr, "");

        for (Node exprCandidate : exprCandidates) {
            SwCase tmp = new SwCase(node);
            tmp.addTrace("SwCase");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }


    /**
     * Mutate switch's expression and block.
     * @param node: switchStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(SwitchStmt node){
        Expr expr = node.getExpression();
        List<Node> exprCandidates = process(expr, "");

        List<Stmt> stmts = node.getStatements();
        Queue<List<Stmt>> building1 = combineAllStmt(stmts);
        List<List<Stmt>> building = new ArrayList<>(building1);

        List<Node> ret = new ArrayList<>();
//        node.addTrace("SwitchStmt");
        ret.add(node);
        if(_combineOption){
            for (Node exprCandidate : exprCandidates) {
                for (List<Stmt> stmtList : building) {
                    SwitchStmt tmp = new SwitchStmt(node);
                    tmp.setExpression((Expr) exprCandidate);
                    tmp.setStatements(stmtList);
                    ret.add(tmp);
                }
            }
        }else{
            for(Node exprCandidate: exprCandidates){
                SwitchStmt tmp = new SwitchStmt(node);
                tmp.addTrace("SwitchStmt");
                tmp.addAllTrace(exprCandidate.getTrace());
                tmp.setExpression((Expr) exprCandidate);
                tmp.setStatements(stmts);
                ret.add(tmp);
            }
            for(List<Stmt> stmtList: building){
                SwitchStmt tmp = new SwitchStmt(node);
                tmp.addTrace("SwitchStmt");
                for (Stmt stmt : stmtList) {
                    tmp.addAllTrace(stmt.getTrace());
                }
                tmp.setExpression(expr);
                tmp.setStatements(stmtList);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }


    /**
     * Mutate this stmt's expr and body part
     * @param node: synchronizedStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(SynchronizedStmt node){
        Expr expr = node.getExpression();
        Blk body = node.getBody();
        List<Node> exprCandidates = process(expr, "");
        List<Node> bodyCandidates = process(body, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("SynchronizedStmt");
        ret.add(node);
        if(_combineOption){
            for (Node exprCandidate : exprCandidates) {
                for (Node bodyCandidate : bodyCandidates) {
                    SynchronizedStmt tmp = new SynchronizedStmt(node);
                    tmp.setExpression((Expr) exprCandidate);
                    tmp.setBlock((Blk) bodyCandidate);
                    ret.add(tmp);
                }
            }
        }else{
            for(Node exprCandidate: exprCandidates){
                SynchronizedStmt tmp = new SynchronizedStmt(node);
                tmp.addTrace("SynchronizedStmt");
                tmp.addAllTrace(exprCandidate.getTrace());
                tmp.setExpression((Expr) exprCandidate);
                tmp.setBlock(body);
                ret.add(tmp);
            }
            for(Node bodyCandidate: bodyCandidates){
                SynchronizedStmt tmp = new SynchronizedStmt(node);
                tmp.addTrace("SynchronizedStmt");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setExpression(expr);
                tmp.setBlock((Blk) bodyCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }


    /**
     * Mutate this node's expression part
     * @param node: throwStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(ThrowStmt node){
        Expr expr = node.getExpression();
        String type = getType(expr);
        List<Node> exprCandidates = process(expr, type);
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ThrowStmt");
        ret.add(node);
        for (Node exprCandidate : exprCandidates) {
            ThrowStmt tmp = new ThrowStmt(node);
            tmp.addTrace("ThrowStmt");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     *Check try-block and cat-Clauses
     *     example:
     *     try [(resources)]
     *         bolock
     *     [{catch clauses}]
     *     [finally block]
     *     only mutate the block and catch clauses
     * @param node: tryStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(TryStmt node){
        Blk tryBlock = node.getBody();
        List<CatClause> clauses = node.getCatches();
        List<Node> bodyCandidates = process(tryBlock, "");
        Queue<List<CatClause>> clausesCombinationTmp = new LinkedList<>();
        if(clauses != null){
            clausesCombinationTmp = combineAllCatch(clauses);
        }
        List<List<CatClause>> clausesCombination = new ArrayList<>(clausesCombinationTmp);
        List<Node> ret = new ArrayList<>();
//        node.addTrace("TryStmt");
        ret.add(node);
        if(_combineOption){
            for (Node bodyCandidate : bodyCandidates) {
                if (clausesCombination.isEmpty()) {
                    TryStmt tmp = new TryStmt(node);
                    tmp.setBody((Blk) bodyCandidate);
                    ret.add(tmp);
                } else {
                    for (List<CatClause> catClauses : clausesCombination) {
                        TryStmt tmp = new TryStmt(node);
                        tmp.setBody((Blk) bodyCandidate);
                        tmp.setCatchClause(catClauses);
                        ret.add(tmp);
                    }
                }
            }
        }else{
            for(Node bodyCandidate: bodyCandidates){
                TryStmt tmp = new TryStmt(node);
                tmp.addTrace("TryStmt");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setBody((Blk) bodyCandidate);
                tmp.setCatchClause(clauses);
                ret.add(tmp);
            }
            for(List<CatClause> catClauses: clausesCombination){
                TryStmt tmp = new TryStmt(node);
                tmp.addTrace("TryStmt");
                for (CatClause catClause : catClauses) {
                    tmp.addAllTrace(catClause.getTrace());
                }
                tmp.setBody(tryBlock);
                tmp.setCatchClause(catClauses);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate catch clause's exception and block.
     * catch ( singleVariableDeclaration )
     *     block
     * @param node: catchClause
     * @return: list of this node's mutations.
     */
    public List<Node> check(CatClause node){
        Svd expr = node.getException();
        Blk body = node.getBody();

        List<Node> exprCandidates = process(expr, "");
        List<Node> bodyCandidates = process(body, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("CatClause");
        ret.add(node);
        if(_combineOption){
            for (Node exprCandidate : exprCandidates) {
                for (Node bodyCandidate : bodyCandidates) {
                    CatClause tmp = new CatClause(node);
                    tmp.setException((Svd) exprCandidate);
                    tmp.setBody((Blk) bodyCandidate);
                    ret.add(tmp);
                }
            }
        }else{
            for(Node exprCandidate: exprCandidates){
                CatClause tmp = new CatClause(node);
                tmp.addTrace("CatClause");
                tmp.addAllTrace(exprCandidate.getTrace());
                tmp.setException((Svd) exprCandidate);
                tmp.setBody(body);
                ret.add(tmp);
            }
            for(Node bodyCandidate: bodyCandidates){
                CatClause tmp = new CatClause(node);
                tmp.addTrace("CatClause");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setException(expr);
                tmp.setBody((Blk) bodyCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip TypeDeclarationStmt
     * @param node: TypeDeclarationStmt
     * @return: list of this node.
     */
    public List<Node> check(TypeDeclarationStmt node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("TypeDeclarationStmt");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate varDeclarationStmt's fragments
     * @param node: varDeclarationStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(VarDeclarationStmt node){
        List<Vdf> fragments = node.getFragments();
        String type = node.getDeclType().getTypeStr();
        Queue<List<Vdf>> building = combineAllVdf(fragments, type); // use type to check each expr's component
        List<Node> ret = new ArrayList<>();
//        node.addTrace("VarDeclarationStmt");
        ret.add(node);
        while(!building.isEmpty()){
            List<Vdf> candidate = building.poll();
            VarDeclarationStmt tmp = new VarDeclarationStmt(node);
            tmp.addTrace("VarDeclarationStmt");
            for(Vdf vdf: candidate){
                tmp.addAllTrace(vdf.getTrace());
            }
            tmp.setFragments(candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate while stmt's expr and body
     * @param node: whileStmt
     * @return: list of this node's mutations.
     */
    public List<Node> check(WhileStmt node){
        Expr expr = node.getExpression();
        Stmt body = node.getBody();
        List<Node> exprCandidates = process(expr, "Boolean");
        List<Node> bodyCandidates = process(body, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("WhileStmt");
        ret.add(node);
        if(_combineOption){
            for (Node exprCandidate : exprCandidates) {
                for (Node bodyCandidate : bodyCandidates) {
                    WhileStmt tmp = new WhileStmt(node);
                    tmp.setExpression((Expr) exprCandidate);
                    tmp.setBody((Stmt) bodyCandidate);
                    ret.add(tmp);
                }
            }
        }else {
            for(Node exprCandidate: exprCandidates){
                WhileStmt tmp = new WhileStmt(node);
                tmp.addTrace("WhileStmt");
                tmp.addAllTrace(exprCandidate.getTrace());
                tmp.setExpression((Expr) exprCandidate);
                tmp.setBody(body);
                ret.add(tmp);
            }
            for(Node bodyCandidate: bodyCandidates){
                WhileStmt tmp = new WhileStmt(node);
                tmp.addTrace("WhileStmt");
                tmp.addAllTrace(bodyCandidate.getTrace());
                tmp.setExpression(expr);
                tmp.setBody((Stmt) bodyCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /************************ Following Are Expressions Mutation...***************************/


    /**
     * Mutate index and array name part.
     * ArrayName[Indexes] or Expression[Expression]
     * @param node: ArrayAccess
     * @param type: type of this node
     * @return: list of this node's mutations.
     */
    public List<Node> check(AryAcc node, String type){
        Expr index = node.getIndex();
        Expr array = node.getArray();
        List<Node> indexCandidates = process(index, "Integer");
        List<Node> arrayCandidates = process(array, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("AryAcc");
        ret.add(node);
        if(_combineOption){
            for (Node indexCandidate : indexCandidates) {
                for (Node arrayCandidate : arrayCandidates) {
                    AryAcc tmp = new AryAcc(node);
                    tmp.setIndex((Expr) indexCandidate);
                    tmp.setArray((Expr) arrayCandidate);
                    ret.add(tmp);
                }
            }
        }else{
            for(Node indexCandidate: indexCandidates){
                AryAcc tmp = new AryAcc(node);
                tmp.addTrace("AryAcc");
                tmp.addAllTrace(indexCandidate.getTrace());
                tmp.setIndex((Expr) indexCandidate);
                tmp.setArray(array);
                ret.add(tmp);
            }
            for(Node arrayCandidate: arrayCandidates){
                AryAcc tmp = new AryAcc(node);
                tmp.addTrace("AryAcc");
                tmp.addAllTrace(arrayCandidate.getTrace());
                tmp.setIndex(index);
                tmp.setArray((Expr) arrayCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate arrayCreation's initializer
     * @param node: arrayCreation
     * @return: list of this node's mutations.
     */
    public List<Node> check(AryCreation node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("AryCreation");
        ret.add(node);

        if(node.getInitializer() == null){
            return ret;
        }

        List<Node> initCandidates = process(node.getInitializer(), "");
        for (Node initCandidate : initCandidates) {
            AryCreation tmp = new AryCreation(node);
            tmp.addTrace("AryCreation");
            tmp.addAllTrace(initCandidate.getTrace());
            tmp.setInitializer((AryInitializer) initCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Mutate arrayInitializer's expressions
     * @param node: arrayInitializer
     * @return: list of this node's mutations.
     */
    public List<Node> check(AryInitializer node){
        List<Expr> exprs = node.getExpressions();
        List<String> types = getTypes(exprs);
        Queue<List<Expr>> building = combineAllExpr(exprs, types);
        List<Node> ret = new ArrayList<>();
//        node.addTrace("AryInitializer");
        ret.add(node);
        while(!building.isEmpty()){
            List<Expr> candidate = building.poll();
            AryInitializer tmp = new AryInitializer(node);
            tmp.addTrace("AryInitializer");
            for(Expr expr: candidate){
                tmp.addAllTrace(expr.getTrace());
            }
            tmp.setExpressions(candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Special take care of rhs is number's situation.
     * @param node: assign
     * @param type: type of this node
     * @return: list of this node's mutations.
     */
    public List<Node> check(Assign node, String type){
        Expr lhs = node.getLhs();
        String ltype = getType(lhs);
        Expr rhs = node.getRhs();
        String rtype = getType(rhs);
        List<Node> lhsCandidates = process(lhs, ltype);
        List<Node> rhsCandidates = process(rhs, rtype);
        if(rhs instanceof NumLiteral){
            // if there is a number literal on right-side, replace it with corresponding type's methodInvocation.
            // StarCoder-lang_45
            if(rtype.equals("Integer")){
                List<Variable> localVars = getUsableVars(node.getStartLine());
                for(Variable var: localVars){
                    if(var.getType().equals("String")){
                        MethodInv methodInv = new MethodInv(rhs.getFileName(), rhs.getStartLine(), rhs.getEndLine(), rhs.getOriNode());
                        SName callerObject = new SName(rhs.getFileName(), rhs.getStartLine(), rhs.getEndLine(), rhs.getOriNode());
                        callerObject.setName(var.getName());
                        methodInv.setExpression(callerObject);
                        SName calleeMethod = new SName(rhs.getFileName(), rhs.getStartLine(), rhs.getEndLine(), rhs.getOriNode());
                        calleeMethod.setName("length");
                        methodInv.setName(calleeMethod);
                        methodInv.addTrace("ADDSTRINGLENGTH");
                        rhsCandidates.add(methodInv);
                    }
                }
            }
        }
        if(rhs instanceof SName){
            PrefixExpr tmp = new PrefixExpr(rhs.getFileName(), rhs.getStartLine(), rhs.getEndLine(), rhs.getOriNode());
            PrefixOperator tmpPrefixOp = new PrefixOperator(rhs.getFileName(), rhs.getStartLine(), rhs.getEndLine(), rhs.getOriNode());
            tmpPrefixOp.setOperatorStr("-");
            tmp.setOperator(tmpPrefixOp);
            tmp.setExpression(rhs);
            tmp.addTrace("Assign");
            rhsCandidates.add(tmp);
        }
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        if(_combineOption){
            for (Node lhsCandidate : lhsCandidates) {
                for (Node rhsCandidate : rhsCandidates) {
                    Assign tmp = new Assign(node);
                    tmp.setLeftHandSide((Expr) lhsCandidate);
                    tmp.setRightHandSide((Expr) rhsCandidate);
                    ret.add(tmp);
                }
            }
        }else{
//            for(Node lhsCandidate: lhsCandidates){
//                Assign tmp = new Assign(node);
//                tmp.addTrace("Assign");
//                tmp.addAllTrace(lhsCandidate.getTrace());
//                tmp.setLeftHandSide((Expr) lhsCandidate);
//                tmp.setRightHandSide(rhs);
//                ret.add(tmp);
//            }
            LevelLogger.debug("rhsCandidates size:"+rhsCandidates);
            for(Node rhsCandidate: rhsCandidates){
                Assign tmp = new Assign(node);
                tmp.addTrace("Assign");
                tmp.addAllTrace(rhsCandidate.getTrace());
                tmp.setLeftHandSide(lhs);
                tmp.setRightHandSide((Expr) rhsCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: AssignOperator
     * @return: List of this node.
     */
    public List<Node> check(AssignOperator node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("AssignOperator");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /*
    Return both true and false
     */
    public List<Node> check(BoolLiteral node){
        boolean diffValue = !node.getValue();
        BoolLiteral tmp = new BoolLiteral(node);
        tmp.setValue(diffValue);
        tmp.addTrace("BoolLiteral");
//        node.addTrace("BoolLiteral");
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        ret.add(tmp);
        return ret;
    }


    /**
     * Skip this node
     * @param node: CastExpr
     * @return: List of this node.
     */
    public List<Node> check(CastExpr node){
        Expr expr = node.getExpresion();
        List<Node> candidates = process(expr, "");
        List<Node> ret = new ArrayList<>();
//        node.addTrace("CastExpr");
        ret.add(node);
        for (Node candidate : candidates) {
            CastExpr tmp = new CastExpr(node);
            tmp.addTrace("CastExpr");
            tmp.addAllTrace(candidate.getTrace());
            tmp.setExpression((Expr) candidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: CharLiteral
     * @return: List of this node.
     */
    public List<Node> check(CharLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("CharLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Select Constructors that parameter numbers close to ClassInstCreation node from all import classes.
     * @param node: ClassInstCreation node
     * @return: List of this node's mutations.
     */
    public List<Node> selectClassInstCreation(ClassInstCreation node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        int MAXCANDIDATESNUM = 10000;
        int parameterNumber = node.getArguments().getExpr().size(); // original parameter amount
        int PARAMETERNUMBERDIFFTHRESHOLD = 2; //argument amount tolerance
        Map<String, List<Variable>> allVars = getAllUsableVars(node.getStartLine());
        // check all import classes
        for(Scope importScope: _importScopes){
            if(node.getStmtParent() instanceof ThrowStmt){
                if(!importScope.getClassName().contains("Exception")){
                    continue;
                }
            }
            MType tmpMtype = new MType(node.getClassType().getFileName(), node.getClassType().getStartLine(), node.getClassType().getEndLine(), node.getClassType().type());
            tmpMtype.setTypeStr(importScope.getClassName());
            LevelLogger.debug("Select Constructor from importScope:"+importScope.getClassName());
            for(ConstructorNode constructor: importScope.getConstructors()){
                List<Variable> parameters = constructor.getParameterList();
                if(constructor.getParameterNums().contains(">=")){ // consider about (args1, arg2, ...) format
                    int candidateParameterNums = Integer.parseInt(constructor.getParameterNums().split(":")[1]);
                    // if parameter amount difference beyond threshold, skip constructor
                    if(parameterNumber < candidateParameterNums && Math.abs(parameterNumber - candidateParameterNums) > PARAMETERNUMBERDIFFTHRESHOLD){
                        continue;
                    }
                    // if parameter > candidateParameterNums, do not know what to fill in... skip
                    if(parameterNumber > candidateParameterNums){
                        continue;
                   }
                }
                else if(Math.abs(parameters.size() - parameterNumber)> PARAMETERNUMBERDIFFTHRESHOLD){
                    continue;
                }
                Queue<List<Variable>> parametersCombination = new LinkedList<>();
                Queue<List<Variable>> parametersCombinationTmp = new LinkedList<>();
                for(Variable var: parameters){
                    // if a parameter's type is not in allVars, skip this constructor
                    if(!allVars.containsKey(var.getType())){
                        break;
                    }
                    // if queue is empty at first, fill the first parameter candidates in queue.
                    if(parametersCombination.isEmpty()){
                       for(Variable typeVar: allVars.get(var.getType())){
                           List<Variable> tmp = new ArrayList<>(); tmp.add(typeVar);
                            parametersCombination.add(tmp);
                       }
                       continue;
                    }
                    // if not empty, each pre will connect to another candidates.
                    while(!parametersCombination.isEmpty()){
                        List<Variable> tmp = parametersCombination.poll();
                        for(Variable typeVar: allVars.get(var.getType())){
                            List<Variable> newTmp = new ArrayList<>(tmp);
                            newTmp.add(typeVar);
                            parametersCombinationTmp.add(newTmp);
                        }
                    }
                    // re-assign memory
                    parametersCombination.addAll(parametersCombinationTmp);
                    parametersCombinationTmp.clear();
                    if(parametersCombination.size() > MAXCANDIDATESNUM) break;
                }

                // if constructor has no parameter or original parameterNumber is zero, add an empty one.
                if(parameters.isEmpty() || parameterNumber == 0){
                    Variable tmp = new Variable(node.getStartLine(), "", "", node);
                    List<Variable> tmpVars = new ArrayList<>();
                    tmpVars.add(tmp);
                    parametersCombination.add(tmpVars);
                }
                // if the parameter size is not zero
                // && the candidate parameter list's parameter amount is not equal to original parameter amount
                // then, generating is failed, skip this constructor.
                if((!parameters.isEmpty() && parametersCombination.peek()==null) || (!parameters.isEmpty() && parametersCombination.peek().size() != parameters.size())){
                    continue;
                }

                for(List<Variable> parameterList: parametersCombination){
                    ClassInstCreation tmp = new ClassInstCreation(node);
                    tmp.setClassType(tmpMtype);
                    ExprList exprList = new ExprList(node.getArguments());
                    List<Expr> exprs = new ArrayList<>();
                    for(Variable var: parameterList){
                        SName tmpExpr = new SName(node.getArguments().getFileName(), node.getArguments().getStartLine(), node.getArguments().getEndLine(), node.getArguments().getOriNode());
                        tmpExpr.setName(var.getName());
                        exprs.add(tmpExpr);
                    }
                    exprList.setExprs(exprs);
                    tmp.setArguments(exprList);
                    tmp.addTrace("ClassInstCreation");
                    ret.add(tmp);
                    if(ret.size() > MAXCANDIDATESNUM) break;
                }
            }
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        LevelLogger.info("Select Constructor from importScope done!");
        return ret;
    }

    /**
     * Mutate ClassInstCreation node by finding possible constructors from import classes.
     * constraint: parameter number should not beyond threshold.
     * @param node: ClassInstCreation node
     * @return: List of this node's mutations.
     */
    public List<Node> check(ClassInstCreation node){
        LevelLogger.debug("ClassInstCreation mutation!!!");
        List<Node> ret = new ArrayList<>(selectClassInstCreation(node));

        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Example: expression ? expression : expression.
     * @param node: ConditionalExpr
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(ConditionalExpr node, String type){
        Expr condition = node.getCondition();
        Expr first     = node.getfirst();
        Expr second    = node.getSecond();
        List<Node> conditionCandidates = process(condition, "Boolean");
        List<Node> firstCandidates     = process(first, type);
        List<Node> secondCandidates    = process(second, type);
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ConditionalExpr");
        ret.add(node);

        if(_combineOption){
            for (Node conditionCandidate : conditionCandidates) {
                for (Node firstCandidate : firstCandidates) {
                    for (Node secondCandidate : secondCandidates) {
                        ConditionalExpr tmp = new ConditionalExpr(node);
                        tmp.setCondition((Expr) conditionCandidate);
                        tmp.setFirst((Expr) firstCandidate);
                        tmp.setSecond((Expr) secondCandidate);
                        ret.add(tmp);
                    }
                }
            }
        }else{
            for(Node conditionCandidate: conditionCandidates){
                ConditionalExpr tmp = new ConditionalExpr(node);
                tmp.addTrace("ConditionalExpr");
                tmp.addAllTrace(conditionCandidate.getTrace());
                tmp.setCondition((Expr) conditionCandidate);
                tmp.setFirst(first);
                tmp.setSecond(second);
                ret.add(tmp);
            }
            for(Node firstCandidate: firstCandidates){
                ConditionalExpr tmp = new ConditionalExpr(node);
                tmp.addTrace("ConditionalExpr");
                tmp.addAllTrace(firstCandidate.getTrace());
                tmp.setCondition(condition);
                tmp.setFirst((Expr) firstCandidate);
                tmp.setSecond(second);
                ret.add(tmp);
            }
            for(Node secondCandidate: secondCandidates){
                ConditionalExpr tmp = new ConditionalExpr(node);
                tmp.addTrace("ConditionalExpr");
                tmp.addAllTrace(secondCandidate.getTrace());
                tmp.setCondition(condition);
                tmp.setFirst(first);
                tmp.setSecond((Expr) secondCandidate);
                ret.add(tmp);
            }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: CreationRef node
     * @return: List of this node.
     */
    public List<Node> check(CreationRef node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("CreationRef");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: Double Literal
     * @return: List of this node.
     */
    public List<Node> check(DoubleLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("DoubleLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: ExpressionMethodRef node
     * @return: List of this node.
     */
    public List<Node> check(ExpressionMethodRef node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("ExpressionMethodRef");
        ret.add(node);
        return ret;
    }

    /**
     * Return the scope where contains given variable
     * @param name: variable name
     * @param type: variable type
     * @return: scope where contains given variable
     */
    public List<Variable> findVar(String name, String type, int lineNUm){
        LevelLogger.debug("Get usable vars in findVar");
        List<Variable> localUsableVars = getUsableVars(lineNUm);
        Variable tmpVar = null;
        List<Variable> ret = new ArrayList<>();
        for(Variable variable: localUsableVars){
            if(variable.getName().equals(name) && typesAreCompatible(variable.getType(), type)){
                tmpVar = variable;
            }else{
                ret.add(variable);
            }
        }
        if(tmpVar != null) return ret;
        return null;

        // check import classes
//        for(Scope scope: _importScopes){
//            tmpVar = scope.containsVar(name);
//            if(tmpVar != null && typesAreCompatible(tmpVar.getType(), type)) return scope;
//        }
    }

    /**
     * FieldAccess is like: a.b.c.d or a.b
     * for a.b, this method can handle it well.
     * for a.b.c.d, this method need to gain ability to trace a->b->c.
     * @param node: FieldAccess node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(FieldAcc node, String type){
        Expr field = node.getExpression();
        SName name = node.getIdentifier();
        List<Node> ret = new ArrayList<>();
//        node.addTrace("FieldAcc");
        ret.add(node);

        if((field instanceof Label)){
            String rangeStr;
            if(field instanceof SName){
                rangeStr = field.toString();
            }else{
                String[] tmp = field.toString().split("\\.");
                rangeStr = tmp[tmp.length-1];
            }
            List<Variable> usableVars= findVar(rangeStr, type, node.getStartLine());
            if(usableVars != null){
                for (Variable usableVar : usableVars) {
                    SName tmpVar = new SName(name.getFileName(), name.getStartLine(), name.getEndLine(), name.getOriNode());
                    tmpVar.setName(usableVar.getName());
                    FieldAcc tmp = new FieldAcc(node);
                    tmp.setIdentifier(tmpVar);
                    tmp.addTrace("FieldAcc");
                    ret.add(tmp);
                }
                return ret;
            }
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: FloatLiteral node
     * @return: List of this node.
     */
    public List<Node> check(FloatLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("FloatLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * InfixExpr can be mutation by its infix operator.
     * if operator is != or ==, then lhs' type and rhs' type are same
     * if operator is <, >, <=, >=, then lhs' type and rhs' type are same
     * if operator is + - * / % ^ & |, <<, >>, >>> then lhs and rhs both Number
     * if operator is && ||, then lhs and rhs both Boolean
     * @param node: InfixExpr node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(InfixExpr node, String type){
        Expr lhs = node.getLhs();
        Expr rhs = node.getRhs();
        List<Node> ret = new ArrayList<>();
//        LevelLogger.debug("LHS:"+lhs);
//        LevelLogger.debug("RHS:"+rhs);
        if(node.getOperator().toString().equals("&&") || node.getOperator().toString().equals("||")){
            type = "Boolean";
        }else if(node.getOperator().toString().contains(">") || node.getOperator().toString().contains("<")) {
            // first, change op
            InfixExpr tmp = new InfixExpr(node);
            tmp.setLeftHandSide(lhs);
            tmp.setRightHandSide(rhs);
            InfixExpr tmp2 = new InfixExpr(node);
            tmp2.setLeftHandSide(lhs);
            tmp2.setRightHandSide(rhs);
            InfixOperator tmpOp = new InfixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
            InfixOperator tmpOp2 = new InfixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
            if(node.getOperator().toString().equals(">")) {
                tmpOp.setOperatorStr("<");
                tmpOp2.setOperatorStr(">=");
            }else if(node.getOperator().toString().equals(">=")) {
                tmpOp.setOperatorStr("<=");
                tmpOp2.setOperatorStr(">");
            }else if(node.getOperator().toString().equals("<")) {
                tmpOp.setOperatorStr(">");
                tmpOp2.setOperatorStr("<=");
            }else if(node.getOperator().toString().equals("<=")) {
                tmpOp.setOperatorStr(">=");
                tmpOp2.setOperatorStr("<");
            }
            tmp.setOperator(tmpOp);
            tmp.addTrace("InfixExpr");
            tmp.addTrace("CMPOP");
            tmp2.setOperator(tmpOp2);
            tmp2.addTrace("InfixExpr");
            tmp2.addTrace("CMPOP");
            ret.add(tmp);
            ret.add(tmp2);
        }
        else{
            type = "";
        }
        List<Node> lhsCandidates = process(lhs, type);
        List<Node> rhsCandidates = process(rhs, type);
//        node.addTrace("InfixExpr");
        ret.add(node);
        if(_combineOption){
            for (Node lhsCandidate : lhsCandidates) {
                for (Node rhsCandidate : rhsCandidates) {
                    InfixExpr expr = new InfixExpr(node);
                    expr.setLeftHandSide((Expr) lhsCandidate);
                    expr.setRightHandSide((Expr) rhsCandidate);
                    ret.add(expr);
                    try {
                        if(type.equals("Boolean")){
                            PrefixExpr tmp = new PrefixExpr(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                            tmp.setExpression((Expr) rhsCandidate);
                            PrefixOperator tmpOp = new PrefixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                            tmpOp.setOperatorStr("!");
                            tmp.setOperator(tmpOp);

                            InfixExpr tmpExpr = new InfixExpr(node);
                            tmpExpr.setLeftHandSide((Expr) lhsCandidate);
                            if(rhs instanceof PrefixExpr){
                                tmpExpr.setRightHandSide((Expr) rhsCandidate);
                            }else{
                                tmpExpr.setRightHandSide(tmp);
                            }
                            ret.add(tmpExpr);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }else{
           for(Node lhsCandidate: lhsCandidates){
               InfixExpr expr = new InfixExpr(node);
               expr.setLeftHandSide((Expr) lhsCandidate);
               expr.setRightHandSide(rhs);
               expr.addTrace("InfixExpr");
               expr.addAllTrace(lhsCandidate.getTrace());
               ret.add(expr);
               if(type.equals("Boolean")){
                   InfixExpr tmp = new InfixExpr(node);

                   InfixOperator tmpEmpty = new InfixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   tmpEmpty.setOperatorStr("");
                   tmp.setOperator(tmpEmpty);

                   SName rhsEmpth = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   rhsEmpth.setName("");
                   tmp.setLeftHandSide((Expr) lhsCandidate);
                   tmp.setOperator(tmpEmpty);
                   tmp.setRightHandSide(rhsEmpth);
                   tmp.addTrace("InfixExpr");
                   tmp.addAllTrace(lhsCandidate.getTrace());
                   tmp.addTrace("RHSEMPTY");
                   ret.add(tmp);
               }
           }
           for(Node rhsCandidate: rhsCandidates){
               InfixExpr expr = new InfixExpr(node);
               expr.setLeftHandSide(lhs);
               expr.setRightHandSide((Expr) rhsCandidate);
               expr.addTrace("InfixExpr");
               expr.addAllTrace(rhsCandidate.getTrace());
               ret.add(expr);

               if(type.equals("Boolean")){
                   PrefixExpr tmp = new PrefixExpr(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   tmp.setExpression((Expr) rhsCandidate);
                   PrefixOperator tmpOp = new PrefixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   tmpOp.setOperatorStr("!");
                   tmp.setOperator(tmpOp);

                   InfixExpr tmpExpr = new InfixExpr(node);
                   tmpExpr.setLeftHandSide(lhs);
                   if(rhs instanceof PrefixExpr){
                       tmpExpr.setRightHandSide((Expr) rhsCandidate);
                   }else{
                       tmpExpr.setRightHandSide(tmp);
                   }
                   tmpExpr.addTrace("InfixExpr");
                   tmpExpr.addTrace("RHSNOT");
                   tmpExpr.addAllTrace(rhsCandidate.getTrace());
                   ret.add(tmpExpr);
               }
               if(type.equals("Boolean")){
                   InfixExpr tmp = new InfixExpr(node);

                   InfixOperator tmpEmpty = new InfixOperator(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   tmpEmpty.setOperatorStr("");
                   tmp.setOperator(tmpEmpty);

                   SName lhsEmpth = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                   lhsEmpth.setName("");
                   tmp.setLeftHandSide(lhsEmpth);
                   tmp.setOperator(tmpEmpty);
                   tmp.setRightHandSide((Expr) rhsCandidate);
                   tmp.addTrace("InfixExpr");
                   tmp.addAllTrace(rhsCandidate.getTrace());
                   tmp.addTrace("LHSEMPTY");
                   ret.add(tmp);
               }
           }
        }

        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node, but there are more things can be done:
     * if operator is relation op, then update op between != and ==, > and >=, < and <=
     * @param node: InfixOperator node
     * @return: List of this node.
     */
    public List<Node> check(InfixOperator node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("InfixOperator");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: InstanceOfExpr node
     * @return: List of this node.
     */
    public List<Node> check(InstanceofExpr node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("InstanceOfExpr");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: IntLiteral node
     * @return: List of this node.
     */
    public List<Node> check(IntLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("IntLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: LambdaExpr node
     * @return: List of this node.
     */
    public List<Node> check(LambdaExpr node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("LambdaExpr");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: LongLiteral node
     * @return: List of this node.
     */
    public List<Node> check(LongLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("LongLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Get all usable variables from _localScope and all import scopes.
     * For variables in import scopes, modify their names to be like: importScopeName.variableName
     * @return: Map of type to variables.
     */
    public Map<String, List<Variable>> getAllUsableVars(int lineNum){
        LevelLogger.debug("Get usable vars in getAllUsableVars");
        List<Variable> allUsableVars = getUsableVars(lineNum);
        Map<String, List<Variable>> allVars = new HashMap<>();
        for(Variable var: allUsableVars){
            if(allVars.containsKey(var.getType())) {
                allVars.get(var.getType()).add(var);
            }else {
                List<Variable> tmp = new ArrayList<>();
                tmp.add(var);
                allVars.put(var.getType(), tmp);
            }
        }
        Map<String, List<Variable>> tmp = new HashMap<>();
        for(Scope importScope: _importScopes){
            tmp = importScope.getType2StaticVars();
            for(String key: tmp.keySet()){
                List<Variable> varsWithField = new ArrayList<>();
                for(Variable var: tmp.get(key)){
                    Variable tmpVar = new Variable(var.getLine(), var.getName(), var.getType(), var.getNode());
                    tmpVar.setName(importScope.getClassName()+"."+var.getName());
                    varsWithField.add(tmpVar);
                }
                if(!allVars.containsKey(key)){
                    allVars.put(key, varsWithField);
                }else{
                    List<Variable> tmpVars = allVars.get(key);
                    tmpVars.addAll(varsWithField);
                    allVars.put(key, tmpVars);
                }
            }
        }
        for(String key: allVars.keySet()){
            Set<Variable> setVars = new HashSet<>(allVars.get(key));
            allVars.put(key, new ArrayList<>(setVars));
        }
        return allVars;
    }

    /**
     * Find functions from specific Scope.
     * Constraint: Candidates' amount of parameters should in the reasonable threshold.
     * @param scope: Scope where contains functions.
     * @param node: MethodInv node
     * @return: List of this node's mutations.
     */
    public List<MethodInv> selectFunctions(Scope scope, MethodInv node, String type) {
        LevelLogger.debug("CURRENT SCOPE:" + scope.getClassName());
        LevelLogger.debug("CURRENT METHODINV:" + node);
        List<FuncSignature> funcs;
        if (scope.equals(_localScope)){
            funcs = getUsableFuncs();
        }else{
            funcs = scope.getFuncs();
        }
        List<Expr> argumentsTmp = node.getArguments().getExpr();
        List<Variable> argumentsOri = new ArrayList<>();
        if (argumentsTmp.isEmpty()) {
            Variable tmp = new Variable(node.getStartLine(), "", "", node);
            argumentsOri.add(tmp);
        } else {
            for (Expr expr : argumentsTmp) {
                Variable tmp = new Variable(expr.getStartLine(), expr.toString(), expr.getTypeStr(), expr);
                argumentsOri.add(tmp);
            }
        }

        List<MethodInv> ret = new ArrayList<>();
        MethodInv tmpMethodInv = new MethodInv(node);
        ret.add(tmpMethodInv);
        Map<String, List<Variable>> allVars = getAllUsableVars(node.getStartLine());
        List<Variable> tmpIntList = new ArrayList<>();
        tmpIntList.add(new Variable(node.getStartLine(), "0", "int", node));
        tmpIntList.add(new Variable(node.getStartLine(), "1", "int", node));
        tmpIntList.add(new Variable(node.getStartLine(), "-1", "int", node));
        if(allVars.containsKey("int")){
            allVars.get("int").addAll(tmpIntList);
        }else{
            allVars.put("int", tmpIntList);
        }
        if(allVars.containsKey("Integer")){
            allVars.get("Integer").addAll(tmpIntList);
        }else {
            allVars.put("Integer", tmpIntList);
        }
        // ADD: if a parameter has method members that can return a object
        //      which type is as same as the original parameter
        //      then add parameter.methodMember() to candidates.
        LevelLogger.debug("BEGIN ADD NEW VARS");
        for (Variable argument : argumentsOri) {
            Scope currentScope = null;
            for (Scope importScope : _importScopes) {
                if (importScope.getClassName().equals(argument.getType())) {
                    currentScope = importScope;
                    break;
                }
            }
            //Node node
            // node.method
            // node.getparent()
            // methodcall()
            if (currentScope == null) continue;
            List<FuncSignature> currentScopeFuncs = currentScope.getFuncs();
            for (FuncSignature func : currentScopeFuncs) {
                if (func.getRetType().equals(argument.getType()) && func.getParameters().size() == 0) {
                    String methodInv = argument.getName() + "." + func.getFuncName() + "()";
                    Variable tmpVar = new Variable(node.getStartLine(), methodInv, argument.getType(), node);
                    if (!allVars.containsKey(argument.getType())) {
                        List<Variable> tmpList = new ArrayList<>();
                        tmpList.add(tmpVar);
                        allVars.put(argument.getType(), tmpList);
                    } else {
                        allVars.get(argument.getType()).add(tmpVar);
                    }
                }
            }
        }

        // Select candidates from current scope's all functions
        int PARAMETERNUMTHRESHOLE = 1;
//        LevelLogger.debug("All variables:"+allVars);
        for (FuncSignature func : funcs) {
            // if function return type is not compatible with expectation
            // or the candidate's amount of arguments exceed the threshold
            // skip it
            if (!typesAreCompatible(func.getRetType(), type) || Math.abs(func.getParameters().size() - node.getArguments().getExpr().size()) > PARAMETERNUMTHRESHOLE) {
                continue;
            }
            SName funcName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
            funcName.setName(func.getFuncName());

            List<Variable> parameters = func.getParameters();
            // one candidate function can have multiple combination of parameters.
            Queue<List<Variable>> parametersCombination = new LinkedList<>();
            Queue<List<Variable>> parametersCombinationTmp = new LinkedList<>();
            Queue<List<Variable>> finalParameters = new LinkedList<>();
            if(parameters.size() == argumentsOri.size() || (func.getParameters().isEmpty() && argumentsTmp.isEmpty())){
                finalParameters.add(argumentsOri);
            }
            if(func.getParameters().isEmpty()){
                Variable tmp = new Variable(node.getStartLine(), "", "", node);
                List<Variable> tmplist = new ArrayList<>();
                finalParameters.add(tmplist);
            }

            if(parameters.size() > 3){
                List<Variable> usedVars = new ArrayList<>();
                for(Expr expr: node.getArguments().getExpr()){
                    String exprType = getType(expr);
                    Variable tmp = new Variable(expr.getStartLine(), expr.toString(), exprType, expr);
                    usedVars.add(tmp);
                }
                allVars = new HashMap<>();
                for(Variable var: usedVars){
                    if(allVars.containsKey(transTypesVerse(var.getType()))) {
                        allVars.get(transTypesVerse(var.getType())).add(var);
                    }else {
                        List<Variable> tmp = new ArrayList<>();
                        tmp.add(var);
                        allVars.put(transTypesVerse(var.getType()), tmp);
                    }
                }
            }

            for (Variable parameter : parameters) {
                if (!allVars.containsKey(parameter.getType())) {
                    break;
                }
                if (parametersCombination.isEmpty()) {
                    for (Variable var : allVars.get(parameter.getType())) {
                        List<Variable> tmp = new ArrayList<>();
                        tmp.add(var);
                        parametersCombination.add(tmp);
                    }
                    continue;
                }
                while (!parametersCombination.isEmpty()) {
                    List<Variable> tmp = parametersCombination.poll();
                    for (Variable var : allVars.get(parameter.getType())) {
                        List<Variable> newTmp = new ArrayList<>(tmp);
                        newTmp.add(var);
                        parametersCombinationTmp.add(newTmp);
                    }
                }
                parametersCombination.addAll(parametersCombinationTmp);
                parametersCombinationTmp.clear();
            }
            finalParameters.addAll(parametersCombination);

            // judge whether the generated parametersCombination is value
            // if original func's parameters' is not zero
            // if the parameterCombination's head node is null
            //    ==> the first parameter can not find.
            // if the parameterCombination's head's size != func's parameters' size
            //    ==> there is a parameter that can not find candidates.
            if ((!parameters.isEmpty() && parametersCombination.peek() == null) || (!parameters.isEmpty() && parametersCombination.peek().size() != parameters.size())) {
                continue;
            }
            parametersCombination.clear(); // all data is in finalParameters, clear redundant data.
            for (List<Variable> parametersCandidates : finalParameters) {
                MethodInv tmp = new MethodInv(node);
                ExprList tmpExprList = new ExprList(node.getArguments());
                List<Expr> tmpExprs = new ArrayList<>();
                for (Variable parameter : parametersCandidates) {
                    SName tmpName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                    tmpName.setName(parameter.getName());
                    tmpExprs.add(tmpName);
                }
                tmpExprList.setExprs(tmpExprs);
                tmp.setName(funcName);
                tmp.setArguments(tmpExprList);
                tmp.addTrace("MethodInv");
                tmp.addTrace("SCOPEMETHODINV");
                ret.add(tmp);
            }
        }
        LevelLogger.debug("SELECTED FUNCTIONS DONE!");
        return ret;
    }

    /**
     * Find functions from a var's inner member.
     * @param node: MethodInv node
     * @param type: type constraint of this node.
     * @param varExpr: var's name
     * @return: List of this node's mutations.
     */
    public List<MethodInv> selectFunctionsForVars(MethodInv node, String type, String varExpr){
        List<MethodInv> ret = new ArrayList<>();
        LevelLogger.debug("Get usable vars in selectFunctionsForVars");
        List<Variable> allUsableVars = getUsableVars(node.getStartLine());
        LevelLogger.debug("Get usable vars in selectFunctionsForVars: "+allUsableVars);
        String varType = "";
        for(Variable var: allUsableVars){
           if(var.getName().equals(varExpr)){
               varType = var.getType();
               LevelLogger.debug("Get usable vars in selectFunctionsForVars: "+varType);
               if(varType.contains(".")){
                     varType = varType.split("\\.")[0];
               }
               break;
           }
        }
        Scope currentScope = null;
        if(_name2Scope.containsKey(varType)) {
            currentScope = _name2Scope.get(varType);
            List<MethodInv> result = selectFunctions(currentScope, node, type);
            LevelLogger.debug("RESULT:"+result);
            for(MethodInv methodInv: result){
                SName tmp = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                tmp.setName(varExpr);
                methodInv.setExpression(tmp);
                methodInv.addTrace("MethodInv");
                methodInv.addTrace("MethodInvForVar");
                ret.add(methodInv);
            }
        }else if (isIterableType(varType)){
            List<String> methodCandaites = getIterableMethodMember(type);
            for(String candidate: methodCandaites){
                MethodInv tmp = new MethodInv(node);
                SName methodName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                methodName.setName(candidate);
                SName varName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                varName.setName(varExpr);
                tmp.setName(methodName);
                tmp.setExpression(varName);
                tmp.addTrace("MethodInv");
                tmp.addTrace("MethodInvForVar(ITERA)");
                ret.add(tmp);
            }
        }else{
            LevelLogger.error("Can not find MethodInvocation's expression's type: "+varType +";"+varExpr);
//            System.exit(-1);
        }
        return ret;
    }

    public List<Node> selectFunctionsForOtherVars(MethodInv node, String type, String varName){
        List<String> methodInvocations = _localScope.getMethodInvocations();
        List<Node> ret = new ArrayList<>();
        for(String inv: methodInvocations){
            String expr = inv.split("\\.")[0];
            if(expr.equals(varName)){
                SName retTmp = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                retTmp.setName(inv);
                retTmp.addTrace("MethodInv");
                retTmp.addTrace("MethodInvForVar(OTHER)");
                ret.add(retTmp);
            }
        }
        return ret;
    }
    /**
     * Try to find functions from _localScope and all import scopes.
     * Constraint: return type and amount of parameters
     * @param node: MethodInv node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(MethodInv node, String type){
        if(node.getExpression() != null){
            List<Node> ret = new ArrayList<>();
//            node.addTrace("MethodInv");
            ret.add(node);
            Expr expr = node.getExpression();
            if(expr instanceof SName){
                // if expr is a var in localScope, try to find functions from localScope
                // and try other Local variables to call corresponding method members.
                LevelLogger.info("Check MethodInvocation: get usable vars...");
                List<Variable> allUsableVars = getUsableVars(node.getStartLine());
                Variable tmpVar = null;
                for(Variable var: allUsableVars){
                    if(var.getName().equals(expr.toString())){
                        tmpVar = var;
                        break;
                    }
                }
                if(tmpVar != null){
                    LevelLogger.info("Check MethodInvocation: searching for variable's method...");
                    // first add all candidates for original var.
                    ret.addAll(selectFunctionsForVars(node, type, expr.toString()));
                    // then, try all other vars.
                    LevelLogger.debug("Check MethodInvocation: all usable vars:"+allUsableVars);
                    for(Variable var: allUsableVars){
//                        if(!var.equals(tmpVar) && Math.abs(var.getLine() - tmpVar.getLine()) < 10){
                            ret.addAll(selectFunctionsForOtherVars(node, type, var.getName()));
//                        }
                    }
                    LevelLogger.info("Check MethodInvocation: variable's method searching done.");
                }else if(_name2Scope.containsKey(expr.toString())){
                    // if expr is an imported class, try to find functions from this scope
                    LevelLogger.info("Check MethodInvocation: searching for imported class' method...");
                    Scope scopePointer = _name2Scope.get(expr.toString());
                    ret.addAll(selectFunctions(scopePointer, node, type));
                    LevelLogger.debug("Check MethodInvocation: Imported class' method searching done.");
                }
                // If the expr is not a variable or imported class, then just try this.methodCall()
                // Try to use keyword `this` substitute expr and find functions from _localScope
                LevelLogger.info("Check MethodInvocation: Searching for this.method...");
                List<MethodInv> currentFileCandidates = selectFunctions(_localScope, node, type);
                for(MethodInv tmp : currentFileCandidates){
                    MethodInv tmp1= new MethodInv(tmp);
                    SName tmpExpr = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getExpression().getOriNode());
                    tmpExpr.setName("this");
                    tmp1.setExpression(tmpExpr);
                    tmp1.addTrace("MethodInv");
                    tmp1.addTrace("THISMETHOD");
                    ret.add(tmp1);
                }
                LevelLogger.info("Check MethodInvocation: this.method searching done.");
            }
            else if(expr instanceof QName){
                // Nothing.
                // TODO: when expr is QName
                //  then find the corresponding type and then find func.
                //  Never meet this situation for now.
            }else if(expr instanceof MethodInv){
                List<MethodInv> currentFileCandidates = selectFunctions(_localScope, (MethodInv) expr, "");
                for(MethodInv tmp : currentFileCandidates){
                    MethodInv tmp1 = new MethodInv(node);
                    tmp1.setExpression((Expr) tmp);
                    tmp1.addTrace("MethodInv");
                    tmp1.addTrace("METHODINVMETHODINV");
                    ret.add(tmp1);
                }
            }else if(expr instanceof ThisExpr){
                ret.addAll(selectFunctions(_localScope, node, type));
            }
            LevelLogger.info("Check MethodInvocation: ranking and filter "+ret.size()+" candidates...");
            ret = rankAndFilterBySimilarityStmt(node, ret);
            LevelLogger.info("Check MethodInvocation: processing done...");
            return ret;
        }
        // if there is no expression part, just find functions from local scope.
        List<Node> ret = new ArrayList<>();
//        node.addTrace("MethodInv");
        ret.add(node);
        LevelLogger.info("Check MethodInvocation: searching for local method...");
        ret.addAll(selectFunctions(_localScope, node, type));

        LevelLogger.info("Check MethodInvocation: ranking and filter "+ret.size()+" candidates methods...");
        ret = rankAndFilterBySimilarityStmt(node, ret);
        LevelLogger.info("Check MethodInvocation: local method searching done.");
        return ret;
    }

    /**
     * Skip this node
     * @param node: MethodRef node
     * @return: List of this node.
     */
    public List<Node> check(MethodRef node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("MethodRef");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: MType node
     * @return: List of this node.
     */
    public List<Node> check(MType node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("MType");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: NullLiteral node
     * @return: List of this node.
     */
    public List<Node> check(NillLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("NillLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: NumLiteral node
     * @return: List of this node.
     */
    public List<Node> check(NumLiteral node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("NumLiteral");
        ret.add(node);
        return ret;
    }

    /**
     * Check expression inner parenthesis.
     * @param node: ParenthesiszedExpr node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(ParenthesiszedExpr node, String type){
        Expr expr = node.getExpression();
        List<Node> exprCandidates = process(expr, type);

        List<Node> ret = new ArrayList<>();
//        node.addTrace("ParenthesiszedExpr");
        ret.add(node);
        for (Node exprCandidate : exprCandidates) {
            ParenthesiszedExpr tmp = new ParenthesiszedExpr(node);
            tmp.addTrace("ParenthesiszedExpr");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpr((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: PostfixExpr node
     * @return: List of this node.
     */
    public List<Node> check(PostfixExpr node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("PostfixExpr");
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: PostfixOperator node
     * @return: List of this node.
     */
    public List<Node> check(PostOperator node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("PostOperator");
        ret.add(node);
        return ret;
    }


    /**
     * PrefixExpr can be mutation by its prefix operator.
     * jdt's prefix ops are: ++, --, +, -, ~, !
     * for now, just give this mutation: ! -> empty
     * @param node: PrefixExpr node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(PrefixExpr node, String type){
        Expr expr = node.getExpression();
        List<Node> exprCandidates = process(expr, type);

        List<Node> ret = new ArrayList<>();
//        node.addTrace("PrefixExpr");
        ret.add(node);
        for (Node exprCandidate : exprCandidates) {
            PrefixExpr tmp = new PrefixExpr(node);
            PrefixExpr tmp2= new PrefixExpr(node);
            tmp.setExpression((Expr) exprCandidate);
            tmp2.setExpression((Expr) exprCandidate);
            if (tmp.getOperator().getOperatorStr().equals("!")) {
                PrefixOperator empty = new PrefixOperator(tmp.getFileName(), tmp.getOperator().getStartLine(), tmp.getOperator().getEndLine(), tmp.getOperator().getOriNode());
                empty.setOperatorStr(" ");
                tmp.setOperator(empty);
            }
            tmp.addTrace("PrefixExpr");
            tmp.addTrace("PREFIXNOT");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp2.addTrace("PrefixExpr");
            tmp2.addAllTrace(exprCandidate.getTrace());
            ret.add(tmp2);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: PrefixOperator node
     * @return: List of this node.
     */
    public List<Node> check(PrefixOperator node){
        List<Node> ret = new ArrayList<>();
//        node.addTrace("PrefixOperator");
        ret.add(node);
        return ret;
    }

    // TODO: check QName
    //  if it exists, then get its type, try all same type vars
    //  else, try all vars
    public List<Node> check(QName node, String type){
        // reAssign type to parse outcome.
        type = parseQName(node.toString());
        Scope scope = null;
        if(_name2Scope.containsKey(type)) {
            scope = _name2Scope.get(type);
        }
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        if(scope != null){
           List<Variable> candidates = scope.getVars();
            for (Variable candidate : candidates) {
                if(!typesAreCompatible(candidate.getType(), type)) continue;
                SName tmp = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                tmp.setName(candidate.toString());
                QName tmp2 = new QName(node);
                tmp2.setSName(tmp);
                tmp2.addTrace("QName");
                ret.add(tmp2);
            }
        }else{
            List<Variable> candidates = getUsableVars(node.getStartLine());
            for (Variable candidate : candidates) {
                if(!typesAreCompatible(candidate.getType(), type)) continue;
                SName tmpSName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
                tmpSName.setName(candidate.getName());
                QName tmp2 = new QName(node);
                tmp2.setSName(tmpSName);
                tmp2.addTrace("QName");
                ret.add(tmp2);
            }
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    // TOADD: check SName
    // if it exists, then get its type, try all same type vars
    // else, try all vars.
    public List<Node> check(SName node, String type){
       List<Variable> usableVars = findVar(node.getName(), type, node.getStartLine());
       List<Node> ret = new ArrayList<>();
       ret.add(node);
       if(usableVars != null){
           for (Variable candidate : usableVars) {
               if(!typesAreCompatible(candidate.getType(), type)) continue;
               SName tmp = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
               tmp.setName(candidate.getName());
               tmp.addTrace("SName");
               ret.add(tmp);
           }
           return ret;
       }else{
           List<Variable> candidates = getUsableVars(node.getStartLine());
           for (Variable candidate : candidates) {
               if(!typesAreCompatible(candidate.getType(), type)) continue;
               SName tmpSName = new SName(node.getFileName(), node.getStartLine(), node.getEndLine(), node.getOriNode());
               tmpSName.setName(candidate.getName());
                tmpSName.addTrace("SName");
               ret.add(tmpSName);
           }
       }

       ret = rankAndFilterBySimilarityStmt(node, ret);
       return ret;
    }

    /**
     * Skip this node
     * @param node: StringLiteral node
     * @return: List of this node.
     */
    public List<Node> check(StrLiteral node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: SuperFieldAcc node
     * @return: List of this node.
     */
    public List<Node> check(SuperFieldAcc node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        ret = rankAndFilterBySimilarityStmt(node, ret);
//        SName name = node.getIdentifier();
//        List<Node> candidates = process(name);
//
//        List<Node> ret = new ArrayList<>();
//
//        for(int i = 0; i<candidates.size(); ++i){
//            SuperFieldAcc tmp = new SuperFieldAcc(node);
//            tmp.setIdentifier((SName) candidates.get(i));
//            ret.add(tmp);
//        }
        return ret;
    }

    /**
     * Skip this node
     * @param node: SuperMethodInv node
     * @return: List of this node
     */
    public List<Node> check(SuperMethodInv node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
       /* SName name = node.getMethodName();
        List<Expr> exprList = node.getArguments().getExpr();

        List<Node> nameCandidates = process(name);
        Queue<List<Expr>> exprTmp = combineAllExpr(exprList);
        List<ExprList> exprCandidates = new ArrayList<>();
        while(exprTmp.size() != 0){
            List<Expr> tmp = exprTmp.poll();
            ExprList exprlist = new ExprList(tmp);
            exprCandidates.add(exprlist);
        }

        List<Node> ret = new ArrayList<>();
        for(int i =0; i<nameCandidates.size(); ++i){
            if(exprCandidates.size() == 0){
                SuperMethodInv tmp = new SuperMethodInv(node);
                tmp.setName((SName) nameCandidates.get(i));
                tmp.setArguments(node.getArguments());
                ret.add(tmp);
            }else{
                for(int j = 0; j < exprCandidates.size(); ++j){
                    SuperMethodInv tmp = new SuperMethodInv(node);
                    tmp.setName((SName) nameCandidates.get(i));
                    tmp.setArguments(exprCandidates.get(j));
                    ret.add(tmp);
                }
            }
        }
        return ret;*/
    }

    /**
     * Skip this node
     * @param node: SuperMethodInv node
     * @return: List of this node
     */
    public List<Node> check(SuperMethodRef node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
    }

    /**
     * Only check the initializer part of a Svd
     * @param node: Svd node
     * @return: List of this node's mutations.
     */
    public List<Node> check(Svd node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        if(node.getInitializer() == null){
            return ret;
        }
        List<Node> initCandidates = process(node.getInitializer(), node.getDeclType().toString());
        for (Node initCandidate : initCandidates) {
            Svd tmp = new Svd(node);
            tmp.addTrace("Svd");
            tmp.addAllTrace(initCandidate.getTrace());
            tmp.setInitializer((Expr) initCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * Skip this node
     * @param node: ThisExpr node
     * @return: List of this node.
     */
    public List<Node> check(ThisExpr node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: SuperMethodInv node
     * @return: List of this node
     */
    public List<Node> check(TyLiteral node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
    }

    /**
     * Skip this node
     * @param node: SuperMethodInv node
     * @return: List of this node
     */
    public List<Node> check(TypeMethodRef node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        return ret;
    }

    /**
     * VarDeclarationExpr is consist of multi(>=1) VarDeclarationFragments.
     * For each VarDeclarationFragment, it will be mutated.
     * @param node: VarDeclarationExpr node
     * @return: List of this node's mutations.
     */
    public List<Node> check(VarDeclarationExpr node){
        List<Node> ret = new ArrayList<>();
        ret.add(node);

        String type = node.getDeclType().typeStr();
        List<Vdf> vdfs= node.getFragments();
        Queue<List<Vdf>> tmpVdfs = combineAllVdf(vdfs, type);
        List<List<Vdf>> vdfCandidates = new ArrayList<>(tmpVdfs);

        for (List<Vdf> vdfCandidate : vdfCandidates) {
            VarDeclarationExpr tmp = new VarDeclarationExpr(node);
            tmp.addTrace("VarDeclarationExpr");
            for(Vdf vdf: vdfCandidate){
                tmp.addAllTrace(vdf.getTrace());
            }
            tmp.setVarDeclFrags(vdfCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    /**
     * vdf is like: id = expr
     * @param node: VarDeclarationFragment node
     * @param type: type constraint of this node.
     * @return: List of this node's mutations.
     */
    public List<Node> check(Vdf node, String type){
        List<Node> ret = new ArrayList<>();
        ret.add(node);
        if(node.getExpression() == null){
            return ret;
        }

        List<Node> exprCandidates = process(node.getExpression(), type);
        for (Node exprCandidate : exprCandidates) {
            Vdf tmp = new Vdf(node);
            tmp.addTrace("Vdf");
            tmp.addAllTrace(exprCandidate.getTrace());
            tmp.setExpression((Expr) exprCandidate);
            ret.add(tmp);
        }
        ret = rankAndFilterBySimilarityStmt(node, ret);
        return ret;
    }

    public List<Node> process(Node node, String requireType){
        // get the node in patch.
        if(node == null){
            return new ArrayList<>();
        }
        if(node instanceof AssertStmt){
            return check((AssertStmt) node);
        }else if(node instanceof Blk){
            return check((Blk) node);
        }else if(node instanceof BreakStmt){
            return check((BreakStmt) node);
        }else if(node instanceof ConstructorInv){
            return check((ConstructorInv) node);
        }else if(node instanceof ContinueStmt){
            return check((ContinueStmt) node);
        }else if(node instanceof DoStmt){
            return check((DoStmt) node);
        }else if(node instanceof EmptyStmt){
            return check((EmptyStmt) node);
        }else if(node instanceof EnhancedForStmt){
            return check((EnhancedForStmt) node);
        }else if(node instanceof ExpressionStmt){
            return check((ExpressionStmt) node);
        }else if(node instanceof ForStmt){
            return check((ForStmt) node);
        }else if(node instanceof IfStmt){
            return check((IfStmt) node);
        }else if(node instanceof ElseStmt){
            return check((ElseStmt) node);
        }else if(node instanceof LabeledStmt){
            return check((LabeledStmt) node);
        }else if(node instanceof ReturnStmt){
            return check((ReturnStmt) node);
        }else if(node instanceof SuperConstructorInv){
            return check((SuperConstructorInv) node);
        }else if(node instanceof SwCase){
            return check((SwCase) node);
        }else if(node instanceof SwitchStmt){
            return check((SwitchStmt) node);
        }else if(node instanceof SynchronizedStmt){
            return check((SynchronizedStmt) node);
        }else if(node instanceof ThrowStmt){
            return check((ThrowStmt) node);
        }else if(node instanceof TryStmt){
            return check((TryStmt) node);
        }else if(node instanceof CatClause){
            return check((CatClause) node);
        }else if(node instanceof TypeDeclarationStmt){
            return check((TypeDeclarationStmt) node);
        }else if(node instanceof VarDeclarationStmt){
            return check((VarDeclarationStmt) node);
        }else if(node instanceof WhileStmt){
            return check((WhileStmt) node);
        }else if(node instanceof AryAcc){
            return check((AryAcc) node, requireType);
        }else if(node instanceof AryCreation){
            return check((AryCreation) node);
        }else if(node instanceof AryInitializer){
            return check((AryInitializer) node);
        }else if(node instanceof Assign){
            return check((Assign) node, requireType);
        }else if(node instanceof AssignOperator){
            return check((AssignOperator) node);
        }else if(node instanceof BoolLiteral){
            return check((BoolLiteral) node);
        }else if(node instanceof CastExpr){
            return check((CastExpr) node);
        }else if(node instanceof CharLiteral) {
            return check((CharLiteral) node);
        }else if(node instanceof ClassInstCreation){
            return check((ClassInstCreation) node);
        }else if(node instanceof ConditionalExpr){
            return check((ConditionalExpr) node, requireType);
        }else if(node instanceof CreationRef){
            return check((CreationRef) node);
        }else if(node instanceof DoubleLiteral){
            return check((DoubleLiteral) node);
        }else if(node instanceof ExpressionMethodRef){
            return check((ExpressionMethodRef) node);
        }else if(node instanceof FieldAcc){
            return check((FieldAcc) node, requireType);
        }else if(node instanceof FloatLiteral){
            return check((FloatLiteral) node);
        }else if(node instanceof InfixExpr){
            return check((InfixExpr) node, requireType);
        }else if(node instanceof InfixOperator){
            return check((InfixOperator) node);
        }else if(node instanceof InstanceofExpr){
            return check((InstanceofExpr) node);
        }else if(node instanceof LambdaExpr){
            return check((LambdaExpr) node);
        }else if(node instanceof LongLiteral){
            return check((LongLiteral) node);
        }else if(node instanceof MethodInv){
            return check((MethodInv) node, requireType);
        }else if(node instanceof MethodRef){
            return check((MethodRef) node);
        }else if(node instanceof MType){
            return check((MType) node);
        }else if(node instanceof NillLiteral){
            return check((NillLiteral) node);
        }else if(node instanceof NumLiteral){
            return check((NumLiteral) node);
        }
//        else if(node instanceof Label){
//            return check((Label) node, requireType);
//        }
        else if(node instanceof ParenthesiszedExpr){
            return check((ParenthesiszedExpr) node, requireType);
        }else if(node instanceof PostfixExpr){
            return check((PostfixExpr) node);
        }else if(node instanceof PostOperator){
            return check((PostOperator) node);
        }else if(node instanceof PrefixExpr){
            return check((PrefixExpr) node, requireType);
        }else if(node instanceof PrefixOperator){
            return check((PrefixOperator) node);
        }else if(node instanceof QName){
            return check((QName) node, requireType);
        }else if(node instanceof SName){
            return check((SName) node, requireType);
        }else if(node instanceof StrLiteral){
            return check((StrLiteral) node);
        }else if(node instanceof SuperFieldAcc){
            return check((SuperFieldAcc) node);
        }else if(node instanceof SuperMethodInv){
            return check((SuperMethodInv) node);
        }else if(node instanceof SuperMethodRef){
            return check((SuperMethodRef) node);
        }else if(node instanceof Svd){
            return check((Svd) node);
        }else if(node instanceof ThisExpr){
            return check((ThisExpr) node);
        }else if(node instanceof TyLiteral){
            return check((TyLiteral) node);
        }else if(node instanceof TypeMethodRef){
            return check((TypeMethodRef) node);
        }else if(node instanceof VarDeclarationExpr){
            return check((VarDeclarationExpr) node);
        }else if(node instanceof Vdf){
            return check((Vdf) node, requireType);
        }else {
//            List<Node> ret = new ArrayList<>();
//            return ret;
            return null;
        }
    }

}
