package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Assign extends Expr {

    private static final long serialVersionUID = 508933142391046341L;
    private Expr _lhs = null;
    private AssignOperator _operator = null;
    private Expr _rhs = null;

    /**
     * Assignment:
     *      Expression AssignmentOperator Expression
     */
    public Assign(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ASSIGN;
    }

    public Assign(Assign another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
        this._operator = another._operator;
    }

    public void setLeftHandSide(Expr lhs) {
        _lhs = lhs;
    }

    public void setOperator(AssignOperator operator) {
        _operator = operator;
    }

    public void setRightHandSide(Expr rhs) {
        _rhs = rhs;
    }

    public AssignOperator getOperator() {
        return _operator;
    }

    public Expr getLhs() {
        return _lhs;
    }

    public Expr getRhs() {
        return _rhs;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(3);
        children.add(_lhs);
        children.add(_operator);
        children.add(_rhs);
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_lhs.toSrcString());
        stringBuffer.append(_operator.toSrcString());
        stringBuffer.append(_rhs.toSrcString());
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_lhs.tokens());
        _tokens.addAll(_operator.tokens());
        _tokens.addAll(_rhs.tokens());
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof Assign) {
            Assign assign = (Assign) other;
            match = _operator.compare(assign._operator) && _lhs.compare(assign._lhs) && _rhs.compare(assign._rhs);
        }
        return match;
    }

}
