package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;

public class DoubleLiteral extends NumLiteral {

    private static final long serialVersionUID = 7468689438028560182L;
    private double _value = 0;
    private final double EPSILON = 1e-7;


    public DoubleLiteral(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.DLITERAL;
    }

    public void setValue(double value) {
        _value = value;
    }

    public double getValue() {
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
    public boolean compare(Node other) {
        if (other != null && other instanceof DoubleLiteral) {
            return (Math.abs(_value - ((DoubleLiteral) other)._value) < EPSILON);
        }
        return false;
    }

}
