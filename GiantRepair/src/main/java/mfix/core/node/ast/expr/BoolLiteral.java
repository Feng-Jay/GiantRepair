package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class BoolLiteral extends Expr {

    private static final long serialVersionUID = 2944431726908480955L;
    private boolean _value = false;

    /**
     * BooleanLiteral:
     *      true false
     */
    public BoolLiteral(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.BLITERAL;
    }

    public BoolLiteral(BoolLiteral another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
    }

    public void setValue(boolean value) {
        _value = value;
    }

    public boolean getValue() {
        return _value;
    }

    @Override
    public StringBuffer toSrcString() {
        return new StringBuffer(String.valueOf(_value));
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(String.valueOf(_value));
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        if (other != null && other instanceof BoolLiteral) {
            return (_value == ((BoolLiteral) other)._value);
        }
        return false;
    }

}
