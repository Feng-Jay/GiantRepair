
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ConstructorInv extends Stmt {

    private static final long serialVersionUID = -680765569439500998L;
    private MType _thisType = null;
    private ExprList _arguments = null;

    /**
     * ConstructorInvocation:
     * [ < Type { , Type } > ]
     * this ( [ Expression { , Expression } ] ) ;
     */
    public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ConstructorInv(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.CONSTRUCTORINV;
    }

    public ConstructorInv(ConstructorInv another){
        this(another._fileName, another._startLine, another._endLine, another.getOriNode(), null);
        List<Expr> tmp = new ArrayList<>();
        this._arguments = another._arguments;
    }

    public String getClassStr() {
        return _thisType == null ? "DUMMY" : _thisType.toSrcString().toString();
    }

    public void setThisType(MType thisType) {
        _thisType = thisType;
    }

    public void setArguments(ExprList arguments) {
        _arguments = arguments;
    }

    public ExprList getArguments() {
        return _arguments;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
//		if(_thisType != null) {
//			stringBuffer.append(_thisType.toSrcString());
//			stringBuffer.append(".");
//		}
        stringBuffer.append("this(");
        stringBuffer.append(_arguments.toSrcString());
        stringBuffer.append(");");
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("(");
        _tokens.addAll(_arguments.tokens());
        _tokens.add(")");
        _tokens.add(";");
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(1);
        children.add(_arguments);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof ConstructorInv) {
            ConstructorInv constructorInv = (ConstructorInv) other;
            match = _arguments.compare(constructorInv._arguments);
        }
        return match;
    }
}
