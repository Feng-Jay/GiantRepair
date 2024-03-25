
package mfix.core.node.ast;

import mfix.common.conf.Constant;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.ast.visitor.NodeVisitor;
import mfix.core.node.check.Tracer;
import mfix.core.node.comp.NodeComparator;
import org.eclipse.jdt.core.dom.ASTNode;

import java.io.Serializable;
import java.util.*;


public abstract class Node implements NodeComparator, Serializable {
    @Override
    public boolean equals(Object o) {
       Node oNode = (Node) o;
       if(oNode == null) return false;
        return this.getFileName().equals(oNode.getFileName()) && this.getStartLine() == oNode.getStartLine() && this.getNodeType().equals(oNode.getNodeType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this._fileName, this.getStartLine(), this.getNodeType());
    }

    private static final long serialVersionUID = -6995771051040337618L;

    /**
     * source file name (with absolute path)
     */
    protected String _fileName;
    /**
     * start line number of current node in the source file
     */
    protected int _startLine;
    /**
     * end line number of current node in the source file
     */
    protected int _endLine;

    protected int _layer;
    /**
     * parent node in the abstract syntax tree
     */
    protected Node _parent;

    protected Node _stmtParent;
    /**
     * enum type of node, for easy comparison
     */
    protected TYPE _nodeType = TYPE.UNKNOWN;
    /**
     * feature vector to represent the subtree rooted current node
     */
//    protected FVector _completeFVector = null;
    /**
     * feature vector of this single node
     */
//    protected FVector _selfFVector = null;

    /**
     * data dependency
     */
    private Node _datadependency;
    /**
     * control dependency
     */
    protected Node _controldependency;
    /**
     * current variable is used by {@code Node} {@code _preUseChain} used previously
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
//    private Node _preUseChain;
    /**
     * current variable will be used by {@code Node} {@code _nextUseChain} used next
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
//    private Node _nextUseChain;
    /**
     * original AST node in the JDT abstract tree model
     * NOTE: AST node dose not support serialization
     */
    protected transient ASTNode _oriNode;
    /**
     * tokenized representation of node
     * NOTE: includes all symbols, e.g., '[', but omit ';'.
     */
    protected transient LinkedList<String> _tokens = null;

    protected double _similarity = 0.0;

    protected Tracer _tracer;

    /**
     * @param fileName  : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine   : end line number of the node in the original source file
     * @param oriNode   : original abstract syntax tree node in the JDT model
     */
    public Node(String fileName, int startLine, int endLine, ASTNode oriNode) {
        this(fileName, startLine, endLine, oriNode, null);
    }

    /**
     * @param fileName  : source file name (with absolute path)
     * @param startLine : start line number of the node in the original source file
     * @param endLine   : end line number of the node in the original source file
     * @param oriNode   : original abstract syntax tree node in the JDT model
     * @param parent    : parent node in the abstract syntax tree
     */
    public Node(String fileName, int startLine, int endLine, ASTNode oriNode, Node parent) {
        _fileName = fileName;
        _startLine = startLine;
        _endLine = endLine;
        _oriNode = oriNode;
        _parent = parent;
        _tracer = new Tracer();
    }

    /**
     * get the original ASTNODE in JDT library.
     * @return: ASTNODE
     */
    public ASTNode getOriNode(){return _oriNode;}

    public double getSimilarity(){return _similarity;}

    public void setSimilarity(double similarity){_similarity = similarity;}

    public void addTrace(String str){_tracer.addTrace(str);}

    public void addAllTrace(List<String> strs){_tracer.addAllTrace(strs);}
    public List<String> getTrace(){return _tracer.getTrace();}

    public Tracer getTracer(){return _tracer;}

    /**
     * get the start line number of node in the original source file
     * @return : line number
     */
    public int getStartLine() {
        return _startLine;
    }

    /**
     * set the start line number of node in the original source file
     * @param line
     */
    public void setStartLine(Integer line){this._startLine = line;}

    /**
     * get the end line number of node in the original source file
     * @return : line number
     */
    public int getEndLine() {
        return _endLine;
    }

    /**
     * set the end line number of node in the original source file
     * @param line: line number
     */
    public void setEndLine(Integer line){this._endLine = line;}

    /**
     * get the length of node in the original source file
     * @return: code length(line number)
     */
    public int getLength(){
        return this.getOriNode().getLength();
    }

    /**
     * get the source file name (with absolute path)
     * @return: code belonging file name
     */
    public String getFileName() {
        return _fileName;
    }

    /**
     * set current node type, {@code Node.TYPE.UNKNOWN} as default
     * @param nodeType : node type
     */
    public void setNodeType(TYPE nodeType) {
        this._nodeType = nodeType;
    }

    /**
     * get node type (see {@code Node.TYPE})
     * @return : current node type
     */
    public TYPE getNodeType() {
        return _nodeType;
    }

    /**
     * set the parent node in the abstract syntax tree
     * @param parent : parent node
     */
    public void setParent(Node parent) {
        this._parent = parent;
    }

    /**
     * set the stmt level parent node in the abstract syntax tree
     * @param parent: parent node in stmt level
     */
    public void setStmtParent(Node parent){this._stmtParent = parent;}

    /**
     * set the depth of node in the abstract syntax tree
     * @param l: depth of node
     */
    public void setLayer(Integer l){ _layer = l;}

    /**
     * get the depth of node in the abstract syntax tree
     * @return: depth of node
     */
    public int getLayer(){return _layer;}

    /**
     * get parent node in the abstract syntax tree
     * @return : parent node
     */
    public Node getParent() {
        return _parent;
    }

    /**
     * get stmt level parent node in the abstract syntax tree
     * @return: parent node in stmt level
     */
    public Node getStmtParent(){return _stmtParent;}

    /**
     * set data dependency of node
     * @param dependency : dependent node, can be {@code null}
     */
    public void setDataDependency(Node dependency) {
        _datadependency = dependency;
    }

    /**
     * get directly data dependency node
     * @return : data dependent node, can be {@code null}
     */
    public Node getDataDependency() {
        return _datadependency;
    }

    /**
     * get all data dependency nodes recursively
     * @param nodes: nodes to get their data dependency
     * @return all data dependency nodes
     */
    public Set<Node> recursivelyGetDataDependency(Set<Node> nodes) {
        if (_datadependency != null) {
            nodes.add(_datadependency);
        }
        if (Constant.EXPAND_PATTERN) {
            for (Node node : getAllChildren()) {
                node.recursivelyGetDataDependency(nodes);
            }
        }
        return nodes;
    }

    /**
     * set control dependency of node
     * @param dependency : dependent node, can be {@code null}
     */
    public void setControldependency(Node dependency) {
        _controldependency = dependency;
    }

    /**
     * get control dependency
     * @return : control dependent node, can be {@code null}
     */
    public Node getControldependency() {
        if (_controldependency == null) return null;
        return _controldependency;
    }

//    public void setPreUsed(Node node) {
//        _preUseChain = node;
//        if (node != null) {
//            node.setNextUsed(this);
//        }
//    }
//    public Node getPreUsed() {
//        return _preUseChain;
//    }
//
//    public void setNextUsed(Node node) {
//        _nextUseChain = node;
//        if (node != null) {
//            node.setPreUsed(this);
//        }
//    }
//    public Node getNextUsed() {
//        return _nextUseChain;
//    }

    /**
     * traverse the complete sub-tree with the given {@code visitor}
     * @param visitor : traverser (visitor pattern)
     */
    public final void accept(NodeVisitor visitor) {
        if (visitor == null) {
            throw new IllegalArgumentException("visitor should not be null!");
        }
        visitor.preVisit(this);

        if (visitor.preVisit(this)) {
            accept0(visitor);
        }
        // end with the generic post-visit
        visitor.postVisit(this);
    }

    /**
     * traverse the sub-tree downwards, used internally only
     * @param visitor : traverser (visitor pattern)
     */
    protected final void accept0(NodeVisitor visitor) {
        if (visitor.visit(this)) {
            for (Node node : getAllChildren()) {
                if (node != null) {
                    node.accept(visitor);
                }
            }
        }
        visitor.endVisit(this);
    }

    /**
     * compute the feature vector for current node
     * @return : feature vector representation
     */
//    public FVector getFeatureVector() {
//        if (_completeFVector == null) {
//            computeFeatureVector();
//        }
//        return _completeFVector;
//    }
//
//    public FVector getSingleFeatureVector() {
//        if (_selfFVector == null) {
//            computeFeatureVector();
//        }
//        return _selfFVector;
//    }
    /**
     * obtain the tokens representation of current node
     * @return
     */
    public List<String> tokens() {
        if (_tokens == null) {
            tokenize();
        }
        return _tokens;
    }

    public int length() {
        return tokens().size();
    }

    /**
     * obtain all defined variables in the sub-tree
     * @return : all variable definition node (see {@code SName})
     */
    public Set<SName> getAllVars() {
        Set<SName> set = new HashSet<>();
        if (this instanceof SName) {
            set.add((SName) this);
        }
        for (Node node : getAllChildren()) {
            set.addAll(node.getAllVars());
        }
        return set;
    }

    /**
     * judge whether this.node is parent of the given {@code node}
     * @param node: node to be judged
     * @return: is parent or not(boolean)
     */
    public boolean isParentOf(Node node) {
        while (node != null) {
            if (node.getParent() == this) {
                return true;
            }
            node = node.getParent();
        }
        return false;
    }

    /**
     * judge whether this.node has data-dependency on the given {@code node}
     * @param node: node to be judged
     * @return: has data-dependency or not(boolean)
     */
    public boolean isDataDependOn(Node node) {
        return getDataDependency() == node;
    }

    /**
     * judge whether this.node has control-dependency on the given {@code node}
     * @param node: node to be judged
     * @return: has control-dependency or not(boolean)
     */
    public boolean isControlDependOn(Node node) {
        return getControldependency() == node;
    }

    /**
     * flatten abstract tree as a list of {@code Node}
     * @param nodes : list contains the {@code Node}
     * @return : a list of {@code Node}s after flattening
     */
    public List<Node> flattenTreeNode(List<Node> nodes) {
        nodes.add(this);
        for (Node node : getAllChildren()) {
            node.flattenTreeNode(nodes);
        }
        return nodes;
    }

    /**
     * recursively get all child {@code Stmt} node
     * @param nodes : a list of child {@code Stmt} node
     * @return : a list of child {@code Stmt} node
     */
    public List<Stmt> getAllChildStmt(List<Stmt> nodes) {
        for (Node node : getAllChildren()) {
            if (node instanceof Stmt) {
                nodes.add((Stmt) node);
                node.getAllChildStmt(nodes);
            }
        }
        return nodes;
    }

    /**
     * recursively get all child {@code Expr} node
     * @param nodes : a list of child {@code Expr} node
     * @return : a list of child {@code Expr} node
     */
    public List<Expr> getAllChildExpr(List<Expr> nodes, boolean filterName) {
        for (Node node : getAllChildren()) {
            if (node instanceof Expr) {
                if (!filterName || !NodeUtils.isSimpleExpr(node)) {
                    nodes.add((Expr) node);
                }
            }
        }
        for (Node node : getAllChildren()) {
            node.getAllChildExpr(nodes, filterName);
        }
        return nodes;
    }

    /**
     * output source code with string format
     * @return : source code string
     */
    public abstract StringBuffer toSrcString();

    /**
     * get (non-direct) parent node that is {@code Stmt} type, maybe itself
     * @return : parent node if exist, otherwise {@code null}
     */
    public abstract Stmt getParentStmt();

    /**
     * get all {@code Stmt} child node, does not include itself
     * NOTE: empty for all {@code Expr} node
     * @return : all child statement
     */
    public abstract List<Stmt> getChildren();

    /**
     * return all child node, does not include itself
     * @return : child node
     */
    public abstract List<Node> getAllChildren();

    /**
     * get all {@code Expr} child nodes' amount, does not include itself
     * @return: amount of child {@code Expr} nodes
     */
    public Integer getChildrenNum(){return getAllChildren().size();}

    /**
     * compute the feature vector for current node recursively
     * and cache the result
     */
//    public abstract void computeFeatureVector();

    /**
     * recursively tokenize the sub abstract syntax tree downwards
     * and cache the result
     */
    protected abstract void tokenize();

    @Override
    public String toString() {
        return toSrcString().toString();
    }

    /*********************************************************/
    /******* record matched information for change ***********/
    /*********************************************************/
    /**
     * bind the target node in the fixed version
     */
//    private Node _bindingNode;
    /**
     * tag whether the node is expanded or not
     */
//    private boolean _expanded = false;
    /**
     * tag whether the node is changed or not after fix
     */
//    private boolean _changed = false;
    /**
     *
     */
//    private boolean _insertDepend = false;
    /**
     * list of modifications bound to the node
     */
//    public boolean isChanged() {
//        return _changed;
//    }
//
//    public void setChanged() {
//        _changed = true;
//    }
//
//    public boolean isExpanded() {
//        return _expanded;
//    }
//
//    public List<Node> wrappedNodes() {
//        return null;
//    }
//
//    public void setExpanded() {
//        _expanded = true;
//    }
//
//    public boolean isInsertDep() {
//        return _insertDepend;
//    }
//
//    public boolean noBinding() {
//        return _bindingNode == null;
//    }
//
//    public boolean noBindingTree() {
//        boolean noBinding = false;
//        if (noBinding()) {
//            noBinding = true;
//            for (Node node : getAllChildren()) {
//                noBinding = noBinding && node.noBindingTree();
//            }
//        }
//        return noBinding;
//    }
//
//
//    public boolean isConsidered() {
//        return _expanded || _changed || _insertDepend;
//    }
//
////    public void setConsidered(boolean considered) {
////        _expanded = considered;
////    }
//
//    public void setInsertDepend(boolean insertDepend) {
//        _insertDepend = insertDepend;
//    }
//
//    public void setBindingNode(Node binding) {
//        if (_bindingNode != null) {
//            _bindingNode._bindingNode = null;
//        }
//        _bindingNode = binding;
//        if (_bindingNode != null) {
//            binding._bindingNode = this;
//        }
//    }
//
//    public Node getBindingNode() {
//        return _bindingNode;
//    }
//
//    /**
//     * obtain the considered node patterns
//     * i.e., all nodes considered based on the data/control dependency
//     * and the structure information (children and parent)
//     *
//     * @param nodes           : all nodes to be considered
//     * @param includeExpanded : tag whether consider the expanded node
//     * @return : a set of nodes
//     */
//    public Set<Node> getConsideredNodesRec(Set<Node> nodes, boolean includeExpanded) {
//        return getConsideredNodesRec(nodes, includeExpanded, new HashSet<>());
//    }
//    public Set<Node> getConsideredNodesRec(Set<Node> nodes, boolean includeExpanded, Set<Node> dependency) {
//        if (noBinding()) {
//            nodes.add(this);
//        } else {
//            if ((includeExpanded && _expanded) || _changed || _insertDepend
//                    || dataDependencyChanged(nodes) || controlDependencyChanged()) {
//                nodes.add(this);
//            }
//        }
//
//        if (!noBindingTree()) {
//            for (Node node : getAllChildren()) {
//                node.getConsideredNodesRec(nodes, includeExpanded);
//            }
//        }
//        return nodes;
//    }
//
//    private boolean dataDependencyChanged(Set<Node> nodes) {
//        if (getDataDependency() == null) {
//            if (_bindingNode.getDataDependency() != null) {
//                return true;
//            }
//        } else if (getDataDependency().getBindingNode()
//                != _bindingNode.getDataDependency()) {
//            if (nodes.contains(getDataDependency())
//                    || nodes.contains(_bindingNode.getDataDependency())) {
//                return false;
//            }
//            // avoid too much dependency changes
//            nodes.add(getDataDependency());
//            nodes.add(_bindingNode.getDataDependency());
//            return !fakeChange(getDataDependency(), _bindingNode.getDataDependency());
//        }
//        return false;
//    }
//
//    private boolean fakeChange(Node d1, Node d2) {
//        Node assigned1 = null;
//        if (d1 instanceof Vdf) {
//            assigned1 = ((Vdf) d1).getExpression();
//        } else if (d1 instanceof Assign) {
//            assigned1 = ((Assign) d1).getRhs();
//        }
//        Node assigned2 = null;
//        if (d2 instanceof Vdf) {
//            assigned2 = ((Vdf) d2).getExpression();
//        } else if (d2 instanceof Assign) {
//            assigned2 = ((Assign) d2).getRhs();
//        }
//        if (assigned1 == null) {
//            return assigned2 == null;
//        } else {
//            return assigned1.getBindingNode() == assigned2;
//        }
//    }
//
//    private boolean controlDependencyChanged() {
////        if (getControldependency() == null) {
////            if (_bindingNode.getControldependency() != null) {
////                return true;
////            }
////        }
////        else if (getControldependency().getBindingNode()
////                != _bindingNode.getControldependency()
////                || _bindingNode.getControldependency() == null) {
////            return true;
////        }
//        return false;
//    }
//
//    /**
//     * expand node considered for match
//     *
//     * @param nodes : considered node set
//     * @return : a set of nodes
//     */
//    public Set<Node> expand(Set<Node> nodes) {
//        expandDependency(nodes);
//        expandBottomUp(nodes);
//        expandTopDown(nodes);
//        return nodes;
//    }
//
//    /**
//     * expand pattern with dependency relations
//     *
//     * @param nodes : considered node set
//     */
//    private void expandDependency(Set<Node> nodes) {
//        if (_datadependency != null) {
//            _datadependency.setExpanded();
//            nodes.add(_datadependency);
//        }
////        if (_controldependency != null) {
////            _controldependency.setConsidered(true);
////            nodes.add(_controldependency);
////        }
//    }
//
//    /**
//     * expand children based on syntax
//     *
//     * @param nodes : considered node set
//     */
//    private void expandTopDown(Set<Node> nodes) {
//        for (Node node : getAllChildren()) {
//            node.setExpanded();
//        }
//        nodes.addAll(getAllChildren());
//    }
//
//    /**
//     * expand parent based on syntax
//     *
//     * @param nodes : considered node set
//     */
//    private void expandBottomUp(Set<Node> nodes) {
//        if (_parent != null) {
//            _parent.setExpanded();
//            nodes.add(_parent);
//        }
//    }
//
//    /**
//     * judging whether the given {@code node} is compatible or not
//     *
//     * @param node : given node
//     * @return : {@code true} is compatible, otherwise {@code false}
//     */
//    protected boolean canBinding(Node node) {
//        return node != null && node.getNodeType() == _nodeType && node.getBindingNode() == null;
//    }
//
//    /**
//     * based on the node binding info, continue to match the child nodes
//     * when parent nodes are not matched
//     */
//    protected void continueTopDownMatchNull() {
//        for (Node node : getAllChildren()) {
//            node.postAccurateMatch(null);
//        }
//    }
//
//    /**
//     * return all modifications bound to the ast node
//     *
//     * @param modifications : a set of to preserve the modifications
//     * @return : a set of modifications
//     */
//    /**
//     * match node after constraint solving
//     *
//     * @param node : node to match
//     * @return : {@code true} is current node matches {@code node}, otherwise {@code false}
//     */
//    public abstract boolean postAccurateMatch(Node node);

    public String getTypeStr() {
        return null;
    }

//    public String getAPIStr() {
//        return null;
//    }
//
//    public String getNameStr() {
//        return null;
//    }


    /*********************************************************/
    /**************** matching buggy code ********************/
    /*********************************************************/

//    private Node _buggyBinding;
//
//    public void setBuggyBindingNode(Node node) {
//        _buggyBinding = node;
//        if (node != null) {
//            node._buggyBinding = this;
//        }
//    }
//
//    public void resetBuggyBinding() {
//        _buggyBinding = null;
//    }
//
//    public Node getBuggyBindingNode() {
//        return _buggyBinding;
//    }
//
//    public boolean noBuggyBinding() {
//        return _buggyBinding == null;
//    }
//
//    public void greedyMatchBinding(Node node, Map<Node, Node> matchedNode,
//                                            Map<String, String> matchedStrings) {
//        NodeUtils.matchSameNodeType(this, node, matchedNode, matchedStrings);
//    }
//
//    public abstract boolean ifMatch(Node node, Map<Node, Node> matchedNode, Map<String, String> matchedStrings,
//                                    MatchLevel level);
    /******************************************************************************************/
    /********************* The following are node type model **********************************/
    /******************************************************************************************/

    //all types of abstract syntax tree node considered currently
    public enum TYPE {

        METHDECL("MethodDeclaration"),
        ARRACC("ArrayAccess"),
        ARRCREAT("ArrayCreation"),
        ARRINIT("ArrayInitilaization"),
        ASSIGN("Assignment"),
        BLITERAL("BooleanLiteral"),
        CAST("CastExpression"),
        CLITERAL("CharacterLiteral"),
        CLASSCREATION("ClassInstanceCreation"),
        COMMENT("Annotation"),
        CONDEXPR("ConditionalExpression"),
        DLITERAL("DoubleLiteral"),
        FIELDACC("FieldAccess"),
        FLITERAL("FloatLiteral"),
        INFIXEXPR("InfixExpression"),
        INSTANCEOF("InstanceofExpression"),
        INTLITERAL("IntLiteral"),
        LABEL("Name"),
        LLITERAL("LongLiteral"),
        MINVOCATION("MethodInvocation"),
        NULL("NullLiteral"),
        NUMBER("NumberLiteral"),
        PARENTHESISZED("ParenthesizedExpression"),
        EXPRSTMT("ExpressionStatement"),
        POSTEXPR("PostfixExpression"),
        PREEXPR("PrefixExpression"),
        QNAME("QualifiedName"),
        SNAME("SimpleName"),
        SLITERAL("StringLiteral"),
        SFIELDACC("SuperFieldAccess"),
        SMINVOCATION("SuperMethodInvocation"),
        SINGLEVARDECL("SingleVariableDeclation"),
        THIS("ThisExpression"),
        TLITERAL("TypeLiteral"),
        VARDECLEXPR("VariableDeclarationExpression"),
        VARDECLFRAG("VariableDeclarationFragment"),
        ANONYMOUSCDECL("AnonymousClassDeclaration"),
        ASSERT("AssertStatement"),
        BLOCK("Block"),
        BREACK("BreakStatement"),
        CONSTRUCTORINV("ConstructorInvocation"),
        CONTINUE("ContinueStatement"),
        DO("DoStatement"),
        EFOR("EnhancedForStatement"),
        FOR("ForStatement"),
        IF("IfStatement"),
        ELSE("ElseStatement"),
        RETURN("ReturnStatement"),
        SCONSTRUCTORINV("SuperConstructorInvocation"),
        SWCASE("SwitchCase"),
        SWSTMT("SwitchStatement"),
        SYNC("SynchronizedStatement"),
        THROW("ThrowStatement"),
        TRY("TryStatement"),
        CATCHCLAUSE("CatchClause"),
        TYPEDECL("TypeDeclarationStatement"),
        VARDECLSTMT("VariableDeclarationStatement"),
        WHILE("WhileStatement"),
        POSTOPERATOR("PostExpression.Operator"),
        INFIXOPERATOR("InfixExpression.Operator"),
        PREFIXOPERATOR("PrefixExpression.Operator"),
        ASSIGNOPERATOR("Assignment.Operator"),
        TYPE("Type"),
        EXPRLST("ExpressionList"),
        UNKNOWN("Unknown");

        private String _name;

        TYPE(String name) {
            _name = name;
        }

        @Override
        public String toString() {
            return _name;
        }
    }

}
