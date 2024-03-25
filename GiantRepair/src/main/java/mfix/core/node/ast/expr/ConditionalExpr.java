package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ConditionalExpr extends Expr {

    private static final long serialVersionUID = -6125079576530376280L;
    private Expr _condition = null;
    private Expr _first = null;
    private Expr _snd = null;

    /**
     * ConditionalExpression:
     *      Expression ? Expression : Expression
     */
    public ConditionalExpr(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CONDEXPR;
    }
    public ConditionalExpr(ConditionalExpr another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
    }

    public void setCondition(Expr condition) {
        _condition = condition;
    }

    public void setFirst(Expr first) {
        _first = first;
    }

    public void setSecond(Expr snd) {
        _snd = snd;
    }

    public Expr getCondition() {
        return _condition;
    }

    public Expr getfirst() {
        return _first;
    }

    public Expr getSecond() {
        return _snd;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_condition.toSrcString());
        stringBuffer.append("?");
        stringBuffer.append(_first.toSrcString());
        stringBuffer.append(":");
        stringBuffer.append(_snd.toSrcString());
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_condition.tokens());
        _tokens.add("?");
        _tokens.addAll(_first.tokens());
        _tokens.add(":");
        _tokens.addAll(_snd.tokens());
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof ConditionalExpr) {
            ConditionalExpr conditionalExpr = (ConditionalExpr) other;
            match = _condition.compare(conditionalExpr._condition) && _first.compare(conditionalExpr._first)
                    && _snd.compare(conditionalExpr._snd);
        }
        return match;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(3);
        children.add(_condition);
        children.add(_first);
        children.add(_snd);
        return children;
    }

}
