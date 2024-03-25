
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ForStmt extends Stmt {

    private static final long serialVersionUID = -377100625221024477L;
    private ExprList _initializers = null;
    private ExprList _updaters = null;
    private Expr _condition = null;
    private Stmt _body = null;

    /**
     * for (
     * [ ForInit ];
     * [ Expression ] ;
     * [ ForUpdate ] )
     * Statement
     * ForInit:
     * Expression { , Expression }
     * ForUpdate:
     * Expression { , Expression }
     */
    public ForStmt(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ForStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.FOR;
    }

    public ForStmt(ForStmt another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
        this._initializers = another._initializers;
        this._updaters     = another._updaters;
    }

    public void setCondition(Expr condition) {
        _condition = condition;
    }

    public Expr getCondition() {
        return _condition;
    }

    public void setInitializer(ExprList initializers) {
        _initializers = initializers;
    }

    public ExprList getInitializer() {
        return _initializers;
    }

    public void setUpdaters(ExprList updaters) {
        _updaters = updaters;
    }

    public ExprList getUpdaters() {
        return _updaters;
    }

    public void setBody(Stmt body) {
        _body = body;
    }

    public Stmt getBody() {
        return _body;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("for(");
        stringBuffer.append(_initializers.toSrcString());
        stringBuffer.append(";");
        if (_condition != null) {
            stringBuffer.append(_condition.toSrcString());
        }
        stringBuffer.append(";");
        stringBuffer.append(_updaters.toSrcString());
        stringBuffer.append(")");
        stringBuffer.append(_body.toSrcString());
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_initializers.tokens());
        _tokens.add(";");
        if (_condition != null) {
            _tokens.addAll(_condition.tokens());
        }
        _tokens.add(";");
        _tokens.addAll(_updaters.tokens());
        _tokens.add(")");
        _tokens.addAll(_body.tokens());
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(4);
        children.add(_initializers);
        if (_condition != null) {
            children.add(_condition);
        }
        children.add(_updaters);
        children.add(_body);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        List<Stmt> children = new ArrayList<>(1);
        if (_body != null) {
            children.add(_body);
        }
        return children;
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof ForStmt) {
            ForStmt forStmt = (ForStmt) other;
            match = _initializers.compare(forStmt._initializers);
            if (_condition != null) {
                match = match && _condition.compare(forStmt._condition);
            } else {
                match = match && (forStmt._condition == null);
            }
            match = match && _updaters.compare(forStmt._updaters);
            match = match && _body.compare(forStmt._body);
        }
        return match;
    }

}
