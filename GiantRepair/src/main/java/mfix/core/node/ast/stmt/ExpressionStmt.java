
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;



public class ExpressionStmt extends Stmt {

    private static final long serialVersionUID = 3654727206887515381L;
    private Expr _expression = null;

    /**
     * ExpressionStatement:
     * StatementExpression ;
     */
    public ExpressionStmt(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ExpressionStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.EXPRSTMT;
    }

    public ExpressionStmt(ExpressionStmt another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
    }

    public void setExpression(Expr expression) {
        _expression = expression;
    }

    public Expr getExpression() {
        return _expression;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_expression.toSrcString());
        stringBuffer.append(";");
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_expression.tokens());
        _tokens.add(";");
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(1);
        children.add(_expression);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        if (other != null && other instanceof ExpressionStmt) {
            ExpressionStmt expressionStmt = (ExpressionStmt) other;
            return _expression.compare(expressionStmt._expression);
        }
        return false;
    }

}
