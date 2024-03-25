package mfix.core.node.check;

import mfix.common.conf.Constant;
import mfix.common.java.Subject;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.*;
import mfix.core.node.ast.stmt.*;
import mfix.core.node.modify.MyActions;
import mfix.core.node.modify.MyInsertion;
import mfix.core.node.modify.MyUpdate;
import mfix.core.node.modify.Wrap;
import org.eclipse.jdt.core.dom.*;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.nio.file.*;

/**
 * This class is used to statically check modification's static correctness.
 */
public class StaticCheck {

    private final String _curFile; // the whole class file

    private final String _funcFile; // the file only contains function itself
    private Scope _localScope; // root scope of the function
    private Scope _curFileScope; // root scope of current file
    private final List<Scope> _importScopes; // scopes of imported files

    private List<String> _imports;

    private List<String> _funcUsedImports;

    private List<String> _memberUsedImports;

    private List<MyActions> _allActions;

    private List<Node> _candidateNodes;

    private Map<MyActions, List<MyActions>> _actions2Candidates;

    Subject _subject;

    /**
     * Constructor: initial all scopes: buggy function -> current file -> import files.
     * @param curFile: the whole class file
     * @param funcFile: the file only contains buggy function
     * @param modifications: the modifications to be checked
     * @param subject: the subject
     */
    public StaticCheck(String curFile, String funcFile, List<MyActions> modifications, Subject subject){
        _curFile = curFile;
        _funcFile= funcFile;
        _allActions = modifications;
        _candidateNodes = new ArrayList<>();
        _subject = subject;
        _actions2Candidates = new HashMap<>();

        // Three level's scope: method, curFile, importFiles
        String[] curFileSplit = curFile.split("/");
        String curFileClass = curFileSplit[curFileSplit.length-1];
        curFileClass  = curFileClass.split(".java")[0];
        _importScopes = new ArrayList<>();

        // Parse buggy method, get local vars.
        parseFunc();
        // Parse buggy file, get usable vars and functions.
        _curFileScope = parseFile();
        // Parse import files, get usable vars and functions.
        parseImports();
    }

    /**
     * Set modifications to be mutated.
     * @param actions: the modifications to be mutated.
     */
    public void setModifications(List<MyActions> actions) {_allActions = actions;}

    /**
     * Get all mutated candidates of current modification.
     * @return: all mutated candidates of current modification.
     */
    public List<Node> getCandidates(){return _candidateNodes;}


    /**
     * check UPDATE & INSERT Actions in _allActions.
     */
    public List<List<MyActions>> check(int MaxNum){
        Queue<List<MyActions>> building = new LinkedList<>();
        if(Constant.COMBINATION_OPTION){
            for(MyActions action: _allActions){
                LevelLogger.info("Current Action:"+action.toString());
                if(building.size() >= MaxNum) break;
                List<MyActions> candidates;
                if(_actions2Candidates.containsKey(action)){
                    candidates = new ArrayList<>(_actions2Candidates.get(action));
                }else{
                    candidates = checkOneAction(action);
                    _actions2Candidates.put(action, candidates);
                }
                List<List<MyActions>> pres = new ArrayList<>(building);
                building.clear();
                if(pres.isEmpty()){
                    for(MyActions candidate: candidates){
                        List<MyActions> tmp = new ArrayList<>();
                        tmp.add(candidate);
                        building.add(tmp);
                    }
                }else{
                    for(List<MyActions> pre: pres){
                        for(MyActions candidate: candidates){
                            List<MyActions> tmp = new ArrayList<>(pre);
                            tmp.add(candidate);
                            building.add(tmp);
                        }
                    }
                }
            }
        }else{
            for(MyActions action: _allActions){
                LevelLogger.info("Current Action:"+action.toString());
                if(building.size() >= MaxNum) break;
                List<MyActions> candidates;
                if(_actions2Candidates.containsKey(action)){
                    candidates = new ArrayList<>(_actions2Candidates.get(action));
                }else{
                    candidates = checkOneAction(action);
                    _actions2Candidates.put(action, candidates);
                }
                int counter1= 0;
                for(MyActions candidate: candidates){
                    counter1 ++;
                    List<MyActions> oneActionList = new ArrayList<>(_allActions);
                    oneActionList.set(_allActions.indexOf(action), candidate);
                    building.add(oneActionList);
                }
            }
        }
        return new ArrayList<>(building);
    }

    public List<MyActions> checkOneAction(MyActions action){
        if(action instanceof MyInsertion){
            return checkInsert(action);
        }
        if(action instanceof MyUpdate){
//            return checkInsert(action);
            return checkUpdate(action);
        }
        if(action instanceof Wrap){
            return checkWrap(action);
        }
        else{
            List<MyActions> ret = new ArrayList<>();
            ret.add(action);
            return ret;
        }
    }

    public List<MyActions> checkInsert(MyActions action){
        List<MyActions> ret = new ArrayList<>();
        Node newNode = action.getCurNode();
        CheckParser checker = new CheckParser(_curFileScope, _localScope, _importScopes, _subject);
        List<Node> candidateNodes = checker.process(newNode, "");
        LevelLogger.info("Candidates' size:"+candidateNodes.size());
        _candidateNodes = candidateNodes;
        for(Node node: candidateNodes) {
            if(action instanceof MyUpdate){
                MyUpdate tmp = new MyUpdate((MyUpdate) action);
                if(hasBlock(node)){
                    String str = blockToExpr(((MyUpdate)action).getFormerNode(),node);
                    tmp.setExprString(str);
                }
                tmp.setTracer(node.getTracer());
                tmp.setCurNode(node);
                tmp.setEditDistance(node.getSimilarity());
                ret.add(tmp);
                continue;
            }else{
                MyInsertion tmp = new MyInsertion((MyInsertion) action);
                tmp.setTracer(node.getTracer());
                tmp.setCurNode(node);
                tmp.setEditDistance(node.getSimilarity());
                ret.add(tmp);
            }

        }
        LevelLogger.debug("CheckInsert Done.");
        return ret;
    }

    public List<MyActions> checkUpdate(MyActions action){
        List<MyActions> ret = new ArrayList<>();
        MyUpdate update = (MyUpdate) action;
        Node oldNode = update.getFormerNode();
        Node newNode = update.getCurNode();
        CheckParser checker0 = new CheckParser(_curFileScope, _localScope, _importScopes, _subject);
        UpdateCheckParser checker = new UpdateCheckParser(checker0);
        List<Node> candidateNodes = checker.process(oldNode, newNode, "");
        LevelLogger.info("Candidates' size:"+candidateNodes.size());
        _candidateNodes = candidateNodes;
        for(Node node: candidateNodes){
            MyUpdate tmp = new MyUpdate((MyUpdate) action);
            tmp.setTracer(node.getTracer());
            tmp.setCurNode(node);
            tmp.setEditDistance(node.getSimilarity());
            ret.add(tmp);
        }
        LevelLogger.debug("CheckUpdate Done.");
        return ret;
    }

    public List<MyActions> checkWrap(MyActions action){
        List<MyActions> ret = new ArrayList<>();
        Wrap wrap = (Wrap) action;
        CheckParser checker = new CheckParser(_curFileScope, _localScope, _importScopes, _subject);
        WrapChecker wrapChecker = new WrapChecker(checker);
        List<Node> candidateNodes = wrapChecker.process(wrap.getCurNode());
        _candidateNodes = candidateNodes;
        for(Node node: candidateNodes){
            Wrap tmp = new Wrap((Wrap) action);
            tmp.setTracer(node.getTracer());
            tmp.setCurNode(node);
            tmp.setEditDistance(node.getSimilarity());
            ret.add(tmp);
        }
        return ret;
    }

    public boolean hasBlock(Node node){
        return node instanceof IfStmt || node instanceof ElseStmt || node instanceof TryStmt || node instanceof CatClause || node instanceof ForStmt || node instanceof WhileStmt || node instanceof EnhancedForStmt || node instanceof DoStmt || node instanceof SwitchStmt;
    }

    public String blockToExpr(Node formerNode, Node node){
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
        return stringBuffer.toString();
    }

    /**
     * Parse buggy function, get all local vars.
     * @return: the root scope of the function.
     */
    public void parseFunc(){
        CompilationUnit cu = JavaFile.genAST(_funcFile);
        CandidateVisitor visitor = new CandidateVisitor(cu, _funcFile, _subject.getHome()+"/"+_subject.getSsrc()+"/");
        cu.accept(visitor);
        Scope rootScope = visitor.getRootScope();
        _funcUsedImports = visitor.getUsedImports();
        Queue<Scope> children = new LinkedList<>();
        children.add(rootScope);
        while(!children.isEmpty()){
            Scope cur = children.poll();
            if(cur.getClassName().startsWith("Method") || cur.getClassName().startsWith("Constructor")){
                _localScope = cur;
                break;
            }else{
                children.addAll(cur.getChildScopes());
            }
        }
//        _localScope.setClassName(_localScope.getClassName().split(":")[1]);
    }

    public void test4ParseScopes(){
        LevelLogger.info("Generating dot file for current file"+_curFile+"...");
        StringBuilder builder = new StringBuilder();
        builder.append("digraph G {\n");
        Queue<Scope> candidates = new LinkedList<>();
        candidates.add(_curFileScope);
        while(!candidates.isEmpty()){
            Scope cur = candidates.poll();
            if(cur.getClassName().startsWith("Method:") || cur.getClassName().startsWith("Constructor:")){
                continue;
            }else{
                List<Scope> childrenScopes = cur.getChildScopes();
                for(Scope child: childrenScopes){
                    builder.append("\""+cur.getClassName()+"\" -> \""+child.getClassName()+"\";\n");
                    candidates.add(child);
                }
            }
        }
        builder.append("}");
        JavaFile.writeStringToFile("./test.dot", builder.toString());
    }

    /**
     * Parse current file, get all usable vars and functions.
     * @return: the root scope of the file.
     */
    public Scope parseFile(){
        // todo: parse whole file, get usable func and variables.
        CompilationUnit cu = JavaFile.genAST(_curFile);
        CandidateVisitor visitor = new CandidateVisitor(cu, _subject.getJavaPath(), _subject.getHome()+"/"+_subject.getSsrc()+"/");
        cu.accept(visitor);
        _imports = visitor.getImports();
        _memberUsedImports = visitor.getMemberUsedImports();
        Set<String> typesSet = new HashSet<>();
        Scope ret = visitor.getRootScope();
        ret.setType2SuperClass(visitor.getType2SuperClass());
        ret.setMethodInvocations(visitor.getMethodInvocations());
        ret.setConstructors(visitor.getConstructors());
        // if there are some types used in this file, go to find them.
        Queue<Scope> children = new LinkedList<>();
        children.add(visitor.getRootScope());
        while(!children.isEmpty()){
            Scope cur = children.poll();
            for(Variable var: cur.getVars()){
                typesSet.add(var.getType());
            }
            children.addAll(cur.getChildScopes());
        }
        List<String> types = new ArrayList<>(typesSet);
        String[] curFileSplit = _curFile.split("/");
        String dirPath = String.join("/", Arrays.asList(curFileSplit).subList(0, curFileSplit.length-1));
        for(String type: types){
            String filePath = dirPath + "/" + type + ".java";
            Path path = Paths.get(_curFile);
            if(Files.exists(path)){
                _imports.add(filePath+";");
            }
        }
        return ret;
    }

    /**
     * After parse curFile, get all import statements.
     * parse import files & used type/Utils files, get usable vars and functions.
     */
    public void parseImports(){
        // Scan co-directory files, add the file contains `Util`/`util` key-word.
        // `Util/util` suffix file added to import if and only if it is used in function.
        Path path = Paths.get(_curFile);
        path = path.getParent();
        try {
            List<Path> subfiles = new ArrayList<>();
            Files.walk(path, 1).filter(Files::isRegularFile).forEach(subfiles::add);
            for(Path file: subfiles){
                String[] filePathSplit = file.toString().split("/");
                String filePath = filePathSplit[filePathSplit.length-1];
                if(filePath.contains("Util") || filePath.contains("util")){
                    String fileName = filePath.split(".java")[0];
                    if(_funcUsedImports.contains(fileName)){
                        _imports.add(file.toString()+";");
                    }
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        Set<String> typesSet = new HashSet<>();
        // if there are some types used in this file, go to find them.
        Queue<Scope> children = new LinkedList<>();
        children.add(_localScope);
        while(!children.isEmpty()){
            Scope cur = children.poll();
            for(Variable var: cur.getVars()){
                typesSet.add(var.getType());
            }
            children.addAll(cur.getChildScopes());
        }

        // Parse import files, get usable vars and functions.
        for(String importContent: _imports){
            String[] importContentSplit = importContent.split("/");
            String classFile = importContentSplit[importContentSplit.length-1];
            classFile = classFile.split(".java;")[0];

            // judge whether the path is the outerClass
            // Tolerance is 3 layers
            String[] pathSplitTmp = importContent.split(".java;")[0].split("/");
            List<String> pathSplit = Arrays.asList(pathSplitTmp);
            List<String> innerClass = new ArrayList<>();
            String curClassName = null;
            for(int i =0; i < 3; ++i){
                importContent = String.join("/", pathSplit.subList(0, pathSplit.size()-i)) + ".java";
                curClassName = pathSplit.get(pathSplit.size()-i-1);
                path = Paths.get(importContent);
                if(Files.exists(path)){
                    break;
                }
                innerClass.add(pathSplit.get(pathSplit.size()-i-1));
            }
            Collections.reverse(innerClass);
            if(innerClass.size() > 2){
                continue;
            }
            if(!curClassName.contains("Exception") && !_memberUsedImports.contains(curClassName) && !_funcUsedImports.contains(curClassName)&&!typesSet.contains(curClassName)){
                if(innerClass.isEmpty())
                    continue;
                else if(!_memberUsedImports.contains(innerClass.get(innerClass.size()-1)) && !_funcUsedImports.contains(innerClass.get(innerClass.size()-1)) && !typesSet.contains(innerClass.get(innerClass.size()-1))) {
                    continue;
                }
            }
//            LevelLogger.debug("Parse import file:"+importContent+"...");
            CompilationUnit cu = JavaFile.genAST(importContent);
            List<AbstractTypeDeclaration> allTypes = cu.types();
            AbstractTypeDeclaration parseType = null;
            for(AbstractTypeDeclaration type: allTypes){
                if((type.getModifiers()&1)==(Modifier.PUBLIC)){
                    parseType = type;
                    break;
                }
            }

            // Use
            ImportVisitor importVisitor = new ImportVisitor(cu, classFile, innerClass);
            if(parseType instanceof TypeDeclaration){
                if(((TypeDeclaration) parseType).getSuperclassType() != null){
                    importVisitor.setSuperClass(((TypeDeclaration) parseType).getSuperclassType().toString());
                }
            }
            if(parseType instanceof EnumDeclaration){
                List<Type> implementsTypes = ((EnumDeclaration) parseType).superInterfaceTypes();
                if(!implementsTypes.isEmpty()){
                    importVisitor.setSuperClass(implementsTypes.get(0).toString());
                }
            }

            cu.accept(importVisitor);
            Scope tmp = null;
            if(innerClass.isEmpty())
                tmp = new Scope(null, classFile);
            else
                tmp = new Scope(null, innerClass.get(innerClass.size()-1));
            tmp.setVars(importVisitor.getVars());
            tmp.setStaticVars(importVisitor.getStaticVars());
            tmp.setType2Vars(importVisitor.getType2Vars());
            tmp.setType2StaticVars(importVisitor.getType2StaticVars());

            tmp.setFunctions(importVisitor.getFuncs());
            tmp.setStaticFuncs(importVisitor.getStaticFuncs());
            tmp.setType2Funcs(importVisitor.getType2Funcs());
            tmp.setType2StaticFuncs(importVisitor.getType2StaticFuncs());

            tmp.setConstructors(importVisitor.getConstructors());
            _importScopes.add(tmp);
//            LevelLogger.debug("Parse import file:"+importContent+" done, scope: "+tmp.getClassName());
        }
    }
}

class CandidateVisitor extends ASTVisitor{

    private final String _fileName;

    private final String _basePath;


    private final CompilationUnit _cu;

    private final List<String> _imports;

    private final List<String> _usedImports;

    private final List<String> _memberUsedImports;

    private final List<String> _methodInvocations;

    private final List<ConstructorNode> _constructors;

    private final Map<String, String> _type2SuperClass;

    private final Scope _rootScope;
    private Scope _currentScope;

    public CandidateVisitor(CompilationUnit cu, String fileName, String basePath){
        _cu = cu;
        _fileName = fileName;
        _basePath = basePath;

        _imports = new ArrayList<>();
        _usedImports = new ArrayList<>();
        _memberUsedImports = new ArrayList<>();
        _methodInvocations = new ArrayList<>();
        _constructors = new ArrayList<>();
        _type2SuperClass = new HashMap<>();

        _rootScope = new Scope(null, _fileName);
        _currentScope = _rootScope;
    }

    public Scope getRootScope(){return _rootScope;}

    public List<String> getImports(){return _imports;}

     public List<String> getUsedImports(){return _usedImports;}

    public List<String> getMemberUsedImports(){return _memberUsedImports;}

    public List<String> getMethodInvocations(){return _methodInvocations;}

    public List<ConstructorNode> getConstructors(){return _constructors;}

    public Map<String, String> getType2SuperClass(){return _type2SuperClass;}
    @Override
    public boolean visit(CompilationUnit node){
        _rootScope.setStartLine(_cu.getLineNumber(node.getStartPosition()));
        _rootScope.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
        return true;
    }

    @Override
    public boolean visit(TypeDeclaration node){
        return true;
    }
    @Override
    public boolean visit(ImportDeclaration node){
        String importContent = node.getName().getFullyQualifiedName();
        // if not the java built-in package, add to import list.
        if (!importContent.startsWith("java.")) {
            String[] splitContentTmp = importContent.split("\\.");
            List<String> splitContent = Arrays.asList(splitContentTmp);
            importContent = _basePath + String.join("/", splitContent.subList(0, splitContent.size())) + ".java;";
            _imports.add(importContent);
        }
        return true;
    }

    @Override
    public boolean visit(FieldDeclaration node){
        String type = node.getType().toString();
        boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;
        _memberUsedImports.add(type);
        for(Object obj: node.fragments()){
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            String varName = fragment.getName().getIdentifier();
            int startline  = _cu.getLineNumber(fragment.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, fragment.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _currentScope.addVar(thisVar);
            if(isStatic){
                _currentScope.addStaticVar(thisVar);
            }
        }
        return true;
    }

    @Override
    public boolean visit(VariableDeclarationStatement node){
        String type = node.getType().toString();
        boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;
        for(Object obj: node.fragments()){
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            String varName = fragment.getName().getIdentifier();
            int startline  = _cu.getLineNumber(fragment.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, fragment.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _currentScope.addVar(thisVar);
            if(isStatic){
                _currentScope.addStaticVar(thisVar);
            }
        }
        return false;
    }

    @Override
    public boolean visit(MethodDeclaration node){
        if(node.isConstructor()){
            ConstructorNode constructorNode = new ConstructorNode(_cu.getLineNumber(node.getStartPosition()), node, _fileName);
            _constructors.add(constructorNode);
        }
        if(node.parameters().isEmpty()){
            return true;
        }
        List<SingleVariableDeclaration> parameters = node.parameters();
        for(SingleVariableDeclaration fragment: parameters){
            String type = fragment.getType().toString();
            String varName = fragment.getName().getIdentifier();
            int startline  = _cu.getLineNumber(fragment.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, fragment.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _currentScope.addVar(thisVar);
        }
        return true;
    }

    @Override
    public boolean visit(CatchClause node){
        SingleVariableDeclaration exception = node.getException();
        String type = exception.getType().toString();
        String varName = exception.getName().getIdentifier();
        int startline  = _cu.getLineNumber(exception.getStartPosition());
        SName name  = new SName(_fileName, startline, startline, exception.getName());
        Variable thisVar = new Variable(startline, varName, type, name);
        _currentScope.addVar(thisVar);
        return true;
    }

    @Override
    public boolean visit(DoStatement node){
        if (!(node.getBody() instanceof Block)) {
            Scope _currentScopeTmp = new Scope(_currentScope, "DoStmtBody");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getBody().getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getBody().getStartPosition()+node.getBody().getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        return true;
    }

    @Override
    public boolean visit(EnhancedForStatement node){
        String type = node.getParameter().getType().toString();
        String varName = node.getParameter().getName().getIdentifier();
        int startline  = _cu.getLineNumber(node.getParameter().getStartPosition());
        SName name  = new SName(_fileName, startline, startline, node.getParameter().getName());
        Variable thisVar = new Variable(startline, varName, type, name);
        _currentScope.addVar(thisVar);
        if(!(node.getBody() instanceof Block)){
            Scope _currentScopeTmp = new Scope(_currentScope, "EnForBody");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getBody().getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getBody().getStartPosition()+node.getBody().getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        return true;
    }

    @Override
    public boolean visit(ForStatement node){
        List<Expression> initalizers = node.initializers();
        for(Expression expr: initalizers){
            if(expr instanceof VariableDeclarationExpression){
                VariableDeclarationExpression assignment = (VariableDeclarationExpression) expr;
                String type = assignment.getType().toString();
                for(VariableDeclarationFragment fragment: (Iterable<VariableDeclarationFragment>)assignment.fragments()){
                    String varName = fragment.getName().getIdentifier();
                    int startline  = _cu.getLineNumber(fragment.getStartPosition());
                    SName name  = new SName(_fileName, startline, startline, fragment.getName());
                    Variable thisVar = new Variable(startline, varName, type, name);
                    _currentScope.addVar(thisVar);
                }
            }
        }
        if(!(node.getBody() instanceof Block)){
            Scope _currentScopeTmp = new Scope(_currentScope, "ForBody");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getBody().getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getBody().getStartPosition()+node.getBody().getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        return true;
    }

    @Override
    public boolean visit(IfStatement node){
        Expression expr = node.getExpression();
        if(expr instanceof VariableDeclarationExpression){
            VariableDeclarationExpression assignment = (VariableDeclarationExpression) expr;
            String type = assignment.getType().toString();
            for(VariableDeclarationFragment fragment: (Iterable<VariableDeclarationFragment>)assignment.fragments()){
                String varName = fragment.getName().getIdentifier();
                int startline  = _cu.getLineNumber(fragment.getStartPosition());
                SName name  = new SName(_fileName, startline, startline, fragment.getName());
                Variable thisVar = new Variable(startline, varName, type, name);
                _currentScope.addVar(thisVar);
            }
        }
        return true;
    }

    @Override
    public boolean visit(SwitchStatement node){
        Expression expression = node.getExpression();
        if(expression instanceof VariableDeclarationExpression){
            VariableDeclarationExpression assignment = (VariableDeclarationExpression) expression;
            String type = assignment.getType().toString();
            for(VariableDeclarationFragment fragment: (Iterable<VariableDeclarationFragment>)assignment.fragments()){
                String varName = fragment.getName().getIdentifier();
                int startline  = _cu.getLineNumber(fragment.getStartPosition());
                SName name  = new SName(_fileName, startline, startline, fragment.getName());
                Variable thisVar = new Variable(startline, varName, type, name);
                _currentScope.addVar(thisVar);
            }
        }
        Scope _currentScopeTmp = new Scope(_currentScope, "SwitchBody");
        _currentScopeTmp.setStartLine(_cu.getLineNumber(((Statement)node.statements().get(0)).getStartPosition()));
        _currentScopeTmp.setEndLine(_cu.getLineNumber(((Statement)node.statements().get(node.statements().size()-1)).getStartPosition()+((Statement)node.statements().get(node.statements().size()-1)).getLength()));
        _currentScope.addChildScope(_currentScopeTmp);
        _currentScope = _currentScopeTmp;
        return true;
    }

    @Override
    public boolean visit(SynchronizedStatement node){
        Expression expression = node.getExpression();
        if(expression instanceof VariableDeclarationExpression){
            VariableDeclarationExpression assignment = (VariableDeclarationExpression) expression;
            String type = assignment.getType().toString();
            for(VariableDeclarationFragment fragment: (Iterable<VariableDeclarationFragment>)assignment.fragments()){
                String varName = fragment.getName().getIdentifier();
                int startline  = _cu.getLineNumber(fragment.getStartPosition());
                SName name  = new SName(_fileName, startline, startline, fragment.getName());
                Variable thisVar = new Variable(startline, varName, type, name);
                _currentScope.addVar(thisVar);
            }
        }
        return true;
    }

    @Override
    public boolean visit(TryStatement node){
        List<CatchClause> catchClauses = node.catchClauses();
        for(CatchClause catchClause: catchClauses){
            SingleVariableDeclaration exception = catchClause.getException();
            String type = exception.getType().toString();
            String varName = exception.getName().getIdentifier();
            int startline  = _cu.getLineNumber(exception.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, exception.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _currentScope.addVar(thisVar);
        }
        return true;
    }

    @Override
    public boolean visit(WhileStatement node){
        Expression expression = node.getExpression();
        if(expression instanceof VariableDeclarationExpression){
            VariableDeclarationExpression assignment = (VariableDeclarationExpression) expression;
            String type = assignment.getType().toString();
            for(VariableDeclarationFragment fragment: (Iterable<VariableDeclarationFragment>)assignment.fragments()){
                String varName = fragment.getName().getIdentifier();
                int startline  = _cu.getLineNumber(fragment.getStartPosition());
                SName name  = new SName(_fileName, startline, startline, fragment.getName());
                Variable thisVar = new Variable(startline, varName, type, name);
                _currentScope.addVar(thisVar);
            }
        }
        if(!(node.getBody() instanceof Block)){
            Scope _currentScopeTmp = new Scope(_currentScope, "WhileBody");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getBody().getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getBody().getStartPosition()+node.getBody().getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        return true;
    }

    @Override
    public void preVisit(ASTNode node){
        Scope _currentScopeTmp = null;
        if(node instanceof TypeDeclaration){
            TypeDeclaration tmpNode = (TypeDeclaration) node;
            _currentScopeTmp = new Scope(_currentScope, tmpNode.getName().toString());
            _currentScopeTmp.setStartLine(_cu.getLineNumber(tmpNode.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(tmpNode.getStartPosition()+tmpNode.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
            if(tmpNode.getSuperclassType() != null){
                _type2SuperClass.put(tmpNode.getName().toString(), tmpNode.getSuperclassType().toString());
            }
            List<Type> superInterfaceTypes = tmpNode.superInterfaceTypes();
            if(superInterfaceTypes != null){
                for(Type interfaceType: superInterfaceTypes){
                    _type2SuperClass.put(tmpNode.getName().toString(), interfaceType.toString());
                }
            }

        }
        else if(node instanceof EnumDeclaration){
            EnumDeclaration tmpNode = (EnumDeclaration) node;
            _currentScopeTmp = new Scope(_currentScope, tmpNode.getName().toString());
            _currentScopeTmp.setStartLine(_cu.getLineNumber(tmpNode.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(tmpNode.getStartPosition()+tmpNode.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof MethodDeclaration){
            MethodDeclaration tmpNode = (MethodDeclaration) node;
            if(tmpNode.isConstructor()) {
                _currentScopeTmp = new Scope(_currentScope, "Constructor:"+tmpNode.getName().toString());
                _currentScopeTmp.setStartLine(_cu.getLineNumber(tmpNode.getStartPosition()));
                _currentScopeTmp.setEndLine(_cu.getLineNumber(tmpNode.getStartPosition()+tmpNode.getLength()));
                _currentScope.addConstructor(new ConstructorNode(_cu.getLineNumber(tmpNode.getStartPosition()), tmpNode, _fileName));
                _currentScope.addChildScope(_currentScopeTmp);
                _currentScope = _currentScopeTmp;
            }else if(tmpNode.getReturnType2() != null){
                _currentScopeTmp = new Scope(_currentScope, "Method:"+tmpNode.getName().toString());
                _currentScopeTmp.setStartLine(_cu.getLineNumber(tmpNode.getStartPosition()));
                _currentScopeTmp.setEndLine(_cu.getLineNumber(tmpNode.getStartPosition()+tmpNode.getLength()));
                FuncSignature thisMethod = new FuncSignature(_cu.getLineNumber(tmpNode.getStartPosition()), tmpNode, _fileName);
                _currentScope.addFunction(thisMethod);
                if((tmpNode.getModifiers() & Modifier.STATIC) != 0){
                    // if function is static;
                    _currentScope.addStaticFunc(thisMethod);
                }
                _currentScope.addChildScope(_currentScopeTmp);
                _currentScope = _currentScopeTmp;
            }else{
                LevelLogger.error("PreVISIT ERROR: Illegal MethodDeclaration!!!");
                return;
            }
        }
        else if(node instanceof Block) {
            _currentScopeTmp = new Scope(_currentScope, "Block");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition() + node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof CatchClause){
            _currentScopeTmp = new Scope(_currentScope, "CatchClause");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof DoStatement){
            _currentScopeTmp = new Scope(_currentScope, "DoStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof EnhancedForStatement) {
            _currentScopeTmp = new Scope(_currentScope, "EnhancedForStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof ForStatement) {
            _currentScopeTmp = new Scope(_currentScope, "ForStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof IfStatement) {
            _currentScopeTmp = new Scope(_currentScope, "IfStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof SwitchStatement){
            _currentScopeTmp = new Scope(_currentScope, "SwitchStmt");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof SynchronizedStatement){
            _currentScopeTmp = new Scope(_currentScope, "SynchronizedStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof TryStatement) {
            _currentScopeTmp = new Scope(_currentScope, "TryStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node instanceof WhileStatement) {
            _currentScopeTmp = new Scope(_currentScope, "WhileStatement");
            _currentScopeTmp.setStartLine(_cu.getLineNumber(node.getStartPosition()));
            _currentScopeTmp.setEndLine(_cu.getLineNumber(node.getStartPosition()+node.getLength()));
            _currentScope.addChildScope(_currentScopeTmp);
            _currentScope = _currentScopeTmp;
        }
        else if(node.getParent() instanceof IfStatement){
            // due to if-statement's then part and else part maybe not block
            // so do some special processes.
            IfStatement parent = (IfStatement) node.getParent();
            if(parent.getThenStatement().equals(node)){
                _currentScopeTmp = new Scope(_currentScope, "IfThen");
                _currentScopeTmp.setStartLine(_cu.getLineNumber(parent.getThenStatement().getStartPosition()));
                _currentScopeTmp.setEndLine(_cu.getLineNumber(parent.getThenStatement().getStartPosition()+parent.getThenStatement().getLength()));
                _currentScope.addChildScope(_currentScopeTmp);
                _currentScope = _currentScopeTmp;
            }
            if(parent.getElseStatement() != null && parent.getElseStatement() == node){
                _currentScopeTmp = new Scope(_currentScope, "IfElse");
                _currentScopeTmp.setStartLine(_cu.getLineNumber(parent.getElseStatement().getStartPosition()));
                _currentScopeTmp.setEndLine(_cu.getLineNumber(parent.getElseStatement().getStartPosition()+parent.getElseStatement().getLength()));
                _currentScope.addChildScope(_currentScopeTmp);
                _currentScope = _currentScopeTmp;
            }
        }
        return;
    }

    public boolean hasBlock(ASTNode node){
        return  node instanceof TypeDeclaration  || node instanceof EnumDeclaration || node instanceof MethodDeclaration || node instanceof Block || node instanceof CatchClause
                || node instanceof DoStatement || node instanceof EnhancedForStatement || node instanceof ForStatement
                || node instanceof IfStatement || node instanceof SwitchStatement || node instanceof SynchronizedStatement
                || node instanceof TryStatement || node instanceof WhileStatement;
    }

    @Override
    public void postVisit(ASTNode node){
        if(hasBlock(node)) {
            _currentScope = _currentScope.getParent();
        }else if(node.getParent() instanceof SwitchStatement){
            SwitchStatement parent = (SwitchStatement) node.getParent();
            if(parent.statements().get(parent.statements().size()-1) == node){
                _currentScope = _currentScope.getParent();
            }
        }
    }

    /*Collect the used import library information*/

    @Override
    public boolean visit(ClassInstanceCreation node){
        String className = node.getType().toString();
        if(!_usedImports.contains(className)){
            _usedImports.add(className);
        }
        return true;
    }

    @Override
    public boolean visit(QualifiedName node){
        String name = node.getFullyQualifiedName();
        if(name.contains(".")){
            String[] nameSplit = name.split("\\.");
            String importName = nameSplit[0];
            if(!_usedImports.contains(importName)){
                _usedImports.add(importName);
            }
        }
        return true;
    }

    @Override
    public boolean visit(MethodInvocation node){
        Expression expression = node.getExpression();
        if(expression instanceof SimpleName){
            String name = ((SimpleName) expression).getIdentifier();
            if(!_usedImports.contains(name)){
                _usedImports.add(name);
            }
            _methodInvocations.add(node.toString());
        }
        return true;
    }
}

class ImportVisitor extends ASTVisitor{
    private final Queue<String> _innerClasses;
    private final CompilationUnit _cu;
    private final String _fileName;
    private String _superClass;
    private int innerClassesIndex = 0;

    private List<Variable> _LocalVars;
    private List<Variable> _StaticVars;

    private Map<String, List<Variable>> _type2LocalVars;
    private Map<String, List<Variable>> _type2StaticVars;

    private List<FuncSignature> _LocalFunctions;
    private List<FuncSignature> _StaticFunctions;

    private Map<String, List<FuncSignature>> _type2LocalFunctions;
    private Map<String, List<FuncSignature>> _type2StaticFunctions;

    private List<ConstructorNode> _constructors;

    public ImportVisitor(CompilationUnit cu, String fileName, List<String> innerClasses){
        _innerClasses = new LinkedList<>(innerClasses);
        _cu = cu;
        _fileName = fileName;

        _LocalVars = new ArrayList<>();
        _StaticVars = new ArrayList<>();

        _type2LocalVars = new HashMap<>();
        _type2StaticVars = new HashMap<>();

        _LocalFunctions = new ArrayList<>();
        _StaticFunctions = new ArrayList<>();

        _type2LocalFunctions = new HashMap<>();
        _type2StaticFunctions = new HashMap<>();

        _constructors = new ArrayList<>();
    }

    public List<Variable> getVars(){
        return _LocalVars;
    }

    public List<Variable> getStaticVars(){
        return _StaticVars;
    }

    public Map<String, List<Variable>> getType2Vars(){
        return _type2LocalVars;
    }

    public Map<String, List<Variable>> getType2StaticVars(){
        return _type2StaticVars;
    }

    public List<FuncSignature> getFuncs(){
        return _LocalFunctions;
    }

    public List<FuncSignature> getStaticFuncs(){
        return _StaticFunctions;
    }

    public Map<String, List<FuncSignature>> getType2Funcs(){
        return _type2LocalFunctions;
    }

    public Map<String, List<FuncSignature>> getType2StaticFuncs(){
        return _type2StaticFunctions;
    }

    public List<ConstructorNode> getConstructors(){
        return _constructors;
    }

    public void setSuperClass(String superClass){
        _superClass = superClass;
    }

    @Override
    public boolean visit(TypeDeclaration node){

        if(_innerClasses.isEmpty()){
            _superClass = node.getSuperclassType() == null ? null : node.getSuperclassType().toString();
            ClassVisitor visitor = new ClassVisitor(_cu, _fileName, _superClass);
            node.accept(visitor);
            _LocalVars = visitor.getVars();
            _StaticVars = visitor.getStaticVars();
            _LocalFunctions = visitor.getFuncs();
            _StaticFunctions = visitor.getStaticFuncs();
            _type2LocalVars = visitor.getType2Vars();
            _type2StaticVars = visitor.getType2StaticVars();
            _type2LocalFunctions = visitor.getType2Funcs();
            _type2StaticFunctions = visitor.getType2StaticFuncs();
            _constructors = visitor.getConstructors();
            return false;
        }
        if(node.getName().toString().equals(_innerClasses.peek())){
            String type = _innerClasses.poll();
            if(_innerClasses.isEmpty()){
                _superClass = node.getSuperclassType() == null ? null : node.getSuperclassType().toString();
                ClassVisitor visitor = new ClassVisitor(_cu, type, _superClass);
                node.accept(visitor);
                _LocalVars = visitor.getVars();
                _StaticVars = visitor.getStaticVars();
                _LocalFunctions = visitor.getFuncs();
                _StaticFunctions = visitor.getStaticFuncs();

                _type2LocalVars = visitor.getType2Vars();
                _type2StaticVars = visitor.getType2StaticVars();
                _type2LocalFunctions = visitor.getType2Funcs();
                _type2StaticFunctions = visitor.getType2StaticFuncs();
                _constructors = visitor.getConstructors();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean visit(EnumDeclaration node){
        if(_innerClasses.isEmpty()){
            List superInterfaceTypes = node.superInterfaceTypes();
            List<String> enumSuperInterface = new ArrayList<>();
            for(Object it: superInterfaceTypes){
                Type superInterface = (Type) it;
                enumSuperInterface.add(superInterface.toString());
            }
            EnumVisitor visitor = new EnumVisitor(_cu, _fileName,enumSuperInterface);
            node.accept(visitor);
            _LocalVars = visitor.getVars();
            _StaticVars = visitor.getStaticVars();
            _LocalFunctions = visitor.getFuncs();
            _StaticFunctions = visitor.getStaticFuncs();

            _type2LocalVars = visitor.getType2Vars();
            _type2StaticVars = visitor.getType2StaticVars();
            _type2LocalFunctions = visitor.getType2Funcs();
            _type2StaticFunctions = visitor.getType2StaticFuncs();
            _constructors = visitor.getConstructors();
            return false;
        }
        if(node.getName().toString().equals(_innerClasses.peek())){
            String type = _innerClasses.poll();
            if(_innerClasses.isEmpty()){
                List superInterfaceTypes = node.superInterfaceTypes();
                List<String> enumSuperInterface = new ArrayList<>();
                for(Object it: superInterfaceTypes){
                    Type superInterface = (Type) it;
                    enumSuperInterface.add(superInterface.toString());
                }
                EnumVisitor visitor = new EnumVisitor(_cu, _fileName, enumSuperInterface);
                node.accept(visitor);
                _LocalVars = visitor.getVars();
                _StaticVars = visitor.getStaticVars();
                _LocalFunctions = visitor.getFuncs();
                _StaticFunctions = visitor.getStaticFuncs();

                _type2LocalVars = visitor.getType2Vars();
                _type2StaticVars = visitor.getType2StaticVars();
                _type2LocalFunctions = visitor.getType2Funcs();
                _type2StaticFunctions = visitor.getType2StaticFuncs();
                _constructors = visitor.getConstructors();
            }
            return true;
        }
        return false;
    }
}

/**
 * This class is used to parse whole class
 * and return variables and functions.
 */
class ClassVisitor extends ASTVisitor{
    private final CompilationUnit _cu;
    private final String _fileName;
    private String _superClass;

    private final List<Variable> _vars;
    private final List<Variable> _staticVars;

    private final List<FuncSignature> _funcs;
    private final List<FuncSignature> _staticFuncs;

    private final List<ConstructorNode> _constructors;

    private final Map<String, List<Variable>> _type2Vars;
    private final Map<String, List<Variable>> _type2StaticVars;

    private final Map<String, List<FuncSignature>> _type2Funcs;
    private final Map<String, List<FuncSignature>> _type2StaticFuncs;

    public ClassVisitor(CompilationUnit cu, String fileName, String superClass){
        _cu = cu;
        _fileName = fileName;
        _superClass = superClass;

        _vars = new ArrayList<>();
        _staticVars = new ArrayList<>();

        _type2Vars = new HashMap<>();
        _type2StaticVars = new HashMap<>();

        _funcs = new ArrayList<>();
        _staticFuncs = new ArrayList<>();

        _type2Funcs = new HashMap<>();
        _type2StaticFuncs = new HashMap<>();

        _constructors = new ArrayList<>();
    }

    public void setSuperClass(String superClass){
        _superClass = superClass;
    }

    public List<Variable> getVars(){
        if(_superClass == null) {
            return _vars;
        }else if(_type2Vars.containsKey(_fileName)){
            for(Variable var: _type2Vars.get(_fileName)){
                Variable tmp = new Variable(var.getLine(), var.getName(), _superClass, var.getNode());
                _vars.add(tmp);
            }
            return _vars;
        }
        return _vars;
    }

    public List<Variable> getStaticVars(){
        if(_superClass == null)
            return _staticVars;
        else if(_type2StaticVars.containsKey(_fileName)){
            for(Variable var: _type2StaticVars.get(_fileName)){
                Variable tmp = new Variable(var.getLine(), var.getName(), _superClass, var.getNode());
                _staticVars.add(tmp);
            }
            return _staticVars;
        }
        return _staticVars;
    }

    public Map<String, List<Variable>> getType2Vars(){
        if(_superClass != null && _type2Vars.containsKey(_fileName)){
            List<Variable> tmpList = new ArrayList<>();
            for(Variable var: _type2Vars.get(_fileName)){
                Variable tmp = new Variable(var.getLine(), var.getName(), _superClass, var.getNode());
                tmpList.add(tmp);
            }
            if(_type2Vars.containsKey(_superClass)) {
                _type2Vars.get(_superClass).addAll(tmpList);
            }else {
                _type2Vars.put(_superClass, tmpList);
            }
            return _type2Vars;
        }
        return _type2Vars;
    }

    public Map<String, List<Variable>> getType2StaticVars(){
        if(_superClass != null && _type2StaticVars.containsKey(_fileName)){
            List<Variable> tmpList = new ArrayList<>();
            for(Variable var: _type2StaticVars.get(_fileName)){
                Variable tmp = new Variable(var.getLine(), var.getName(), _superClass, var.getNode());
                tmpList.add(tmp);
            }
            if(_type2StaticVars.containsKey(_superClass)) {
                _type2StaticVars.get(_superClass).addAll(tmpList);
            }else {
                _type2StaticVars.put(_superClass, tmpList);
            }
            return _type2StaticVars;
        }
        return _type2StaticVars;
    }

    public List<FuncSignature> getFuncs(){return _funcs;}

    public List<FuncSignature> getStaticFuncs(){return _staticFuncs;}

    public Map<String, List<FuncSignature>> getType2Funcs(){return _type2Funcs;}

    public Map<String, List<FuncSignature>> getType2StaticFuncs(){return _type2StaticFuncs;}

    public List<ConstructorNode> getConstructors(){return _constructors;}

    @Override
    public boolean visit(FieldDeclaration node){
        String type = node.getType().toString();
        boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;

        for(Object obj: node.fragments()){
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            String varName = fragment.getName().getIdentifier();
            int startline  = _cu.getLineNumber(fragment.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, fragment.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _vars.add(thisVar);
            if(_type2Vars.containsKey(type)) {
                _type2Vars.get(type).add(thisVar);
            }else{
                List<Variable> tmp = new ArrayList<>();
                tmp.add(thisVar);
                _type2Vars.put(type, tmp);
            }

            if(isStatic){
                _staticVars.add(thisVar);
                if(_type2StaticVars.containsKey(type)) {
                    _type2StaticVars.get(type).add(thisVar);
                }
                else{
                    List<Variable> tmp = new ArrayList<>();
                    tmp.add(thisVar);
                    _type2StaticVars.put(type, tmp);
                }
            }
        }
        return true;
    }

    @Override
    public boolean visit(MethodDeclaration node){
        if(node.isConstructor()){
            _constructors.add(new ConstructorNode(_cu.getLineNumber(node.getStartPosition()), node, _fileName));
            return true;
        }else if(node.getReturnType2() != null){
            String retType = node.getReturnType2().toString();
            FuncSignature thisMethod = new FuncSignature(_cu.getLineNumber(node.getStartPosition()), node, _fileName);
            _funcs.add(thisMethod);
            if(_type2Funcs.containsKey(retType)) {
                _type2Funcs.get(retType).add(thisMethod);
            }else {
                List<FuncSignature> tmp = new ArrayList<>();
                tmp.add(thisMethod);
                _type2Funcs.put(retType, tmp);
            }
            if((node.getModifiers() & Modifier.STATIC) != 0){
                _staticFuncs.add(thisMethod);
                if(_type2StaticFuncs.containsKey(retType)){
                    _type2StaticFuncs.get(retType).add(thisMethod);
                }else{
                    List<FuncSignature> tmp = new ArrayList<>();
                    tmp.add(thisMethod);
                    _type2StaticFuncs.put(retType, tmp);
                }
            }
            return true;
        }else{
            return false;
        }
    }

}

class EnumVisitor extends ASTVisitor{

    private final String _fileName;

    private List<String> _superClass = null;

    private final List<Variable> _vars; // line to var
    private final List<Variable> _staticVars;
    private final Map<String, List<Variable>> _type2vars; // type to vars
    private final Map<String, List<Variable>> _type2StaticVars;

    private final List<FuncSignature> _funcs; // functions
    private final List<FuncSignature> _staticFuncs;
    private final Map<String, List<FuncSignature>> _type2funcs; // type to funcs
    private final Map<String, List<FuncSignature>> _type2StaticFuncs;
    private final List<ConstructorNode> _constructors;

    private final CompilationUnit _cu;

    public EnumVisitor(CompilationUnit cu, String fileName, List<String> superClass){
        _cu = cu;
        _fileName = fileName;
        _superClass = superClass;

        _vars = new ArrayList<>();
        _staticVars = new ArrayList<>();
        _type2vars = new HashMap<>();
        _type2StaticVars = new HashMap<>();

        _funcs= new ArrayList<>();
        _staticFuncs = new ArrayList<>();
        _type2funcs = new HashMap<>();
        _type2StaticFuncs = new HashMap<>();

        _constructors = new ArrayList<>();
    }

    public void setSuperClass(List<String> sp){_superClass = sp;}

    public List<Variable> getVars(){
        if(!_superClass.isEmpty() && _type2vars.containsKey(_fileName)){
            for(Variable var: _type2vars.get(_fileName)){
                for(String sp: _superClass){
                    Variable tmp = new Variable(var.getLine(), var.getName(), sp, var.getNode());
                    _vars.add(tmp);
                }
            }
            return _vars;
        }
        return _vars;
    }

    public List<Variable> getStaticVars(){
        if(!_superClass.isEmpty() && _type2StaticVars.containsKey(_fileName)){
            for(Variable var: _type2StaticVars.get(_fileName)){
                for(String sp: _superClass){
                    Variable tmp = new Variable(var.getLine(), var.getName(), sp, var.getNode());
                    _staticVars.add(tmp);
                }
            }
            return _staticVars;
        }
        return _staticVars;
    }

    public Map<String, List<Variable>> getType2Vars(){
        if(!_superClass.isEmpty() && _type2vars.containsKey(_fileName)){
            for(String sp: _superClass){
                List<Variable> tmpList = new ArrayList<>();
                for(Variable var: _type2vars.get(_fileName)){
                    Variable tmp = new Variable(var.getLine(), var.getName(), sp, var.getNode());
                    tmpList.add(tmp);
                }
                if(_type2vars.containsKey(sp)) {
                    _type2vars.get(sp).addAll(tmpList);
                }else {
                    _type2vars.put(sp, tmpList);
                }
            }

            return _type2vars;
        }
        return _type2vars;
    }

    public Map<String, List<Variable>> getType2StaticVars(){
        if(!_superClass.isEmpty() && _type2StaticVars.containsKey(_fileName)){
            for(String sp: _superClass){
                List<Variable> tmpList = new ArrayList<>();
                for(Variable var: _type2StaticVars.get(_fileName)){
                    Variable tmp = new Variable(var.getLine(), var.getName(), sp, var.getNode());
                    tmpList.add(tmp);
                }
                if(_type2StaticVars.containsKey(sp)) {
                    _type2StaticVars.get(sp).addAll(tmpList);
                }else {
                    _type2StaticVars.put(sp, tmpList);
                }
            }
            return _type2StaticVars;
        }
        return _type2StaticVars;
    }

    public List<FuncSignature> getFuncs(){return _funcs;}

    public List<FuncSignature> getStaticFuncs(){return _staticFuncs;}

    public Map<String, List<FuncSignature>> getType2Funcs(){return _type2funcs;}

    public Map<String, List<FuncSignature>> getType2StaticFuncs(){return _type2StaticFuncs;}

    public List<ConstructorNode> getConstructors(){return _constructors;}

    @Override
    public boolean visit(TypeDeclaration node){
        boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;
        return !isStatic;
    }
    @Override
    public boolean visit(FieldDeclaration node){
        String type = node.getType().toString();
        boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;
        for(Object obj: node.fragments()){
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) obj;
            String varName = fragment.getName().getIdentifier();
            int startline  = _cu.getLineNumber(fragment.getStartPosition());
            SName name  = new SName(_fileName, startline, startline, fragment.getName());
            Variable thisVar = new Variable(startline, varName, type, name);
            _vars.add(thisVar);
            if(_type2vars.containsKey(type)){
                _type2vars.get(type).add(thisVar);
            }else{
                List<Variable> tmp = new ArrayList<>();
                tmp.add(thisVar);
                _type2vars.put(type, tmp);
            }
            if(isStatic){
                _staticVars.add(thisVar);
                if(_type2StaticVars.containsKey(type)){
                    _type2StaticVars.get(type).add(thisVar);
                }else{
                    List<Variable> tmp = new ArrayList<>();
                    tmp.add(thisVar);
                    _type2StaticVars.put(type, tmp);
                }
            }
        }
        return true;
    }
    @Override
    public boolean visit(MethodDeclaration node){
        if(node.isConstructor()){
            _constructors.add(new ConstructorNode(_cu.getLineNumber(node.getStartPosition()), node, _fileName));
            return true;
        }else if (node.getReturnType2() != null){
            String returnType = node.getReturnType2().toString();
            boolean isStatic = (node.getModifiers() & Modifier.STATIC) != 0;
            FuncSignature thisfunc = new FuncSignature(_cu.getLineNumber(node.getStartPosition()), node, _fileName);
            _funcs.add(thisfunc);
            if(_type2funcs.containsKey(returnType)) {
                _type2funcs.get(returnType).add(thisfunc);
            }else{
                List<FuncSignature> tmp = new ArrayList<>();
                tmp.add(thisfunc);
                _type2funcs.put(returnType, tmp);
            }
            if(isStatic){
                _staticFuncs.add(thisfunc);
                if(_type2StaticFuncs.containsKey(returnType)){
                    _type2StaticFuncs.get(returnType).add(thisfunc);
                }else{
                    List<FuncSignature> tmp = new ArrayList<>();
                    tmp.add(thisfunc);
                    _type2StaticFuncs.put(returnType, tmp);
                }
            }
            return true;
        }else{
            return false;
        }
    }

    @Override
    public boolean visit(EnumConstantDeclaration node){
//        LevelLogger.debug("EnumConstantDeclaration:"+node);
        String name = node.getName().toString();
        String type = _fileName;
        int startline = _cu.getLineNumber(node.getStartPosition());
        SName sName = new SName(_fileName, startline, startline, node.getName());
        Variable thisVar = new Variable(startline, name, type, sName);
        _vars.add(thisVar);
        if(_type2vars.containsKey(type)){
            _type2vars.get(type).add(thisVar);
        }else{
            List<Variable> tmp = new ArrayList<>();
            tmp.add(thisVar);
            _type2vars.put(type, tmp);
        }

        _staticVars.add(thisVar);
        if(_type2StaticVars.containsKey(type)){
            _type2StaticVars.get(type).add(thisVar);
        }else{
            List<Variable> tmp = new ArrayList<>();
            tmp.add(thisVar);
            _type2StaticVars.put(type, tmp);
        }
        return true;
    }
}
