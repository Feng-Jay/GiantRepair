package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;

import java.util.LinkedList;

public class AssignOperator extends Operator {

    private static final long serialVersionUID = 2573726544838821670L;
    private String _operatorStr;
    private transient Assignment.Operator _operator;

    public AssignOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        _nodeType = TYPE.ASSIGNOPERATOR;
    }

    public void setOperator(Assignment.Operator operator) {
        this._operator = operator;
        this._operatorStr = operator.toString();
    }

    public Assignment.Operator getOperator() {
        return _operator;
    }

    public String getOperatorStr() {
        return _operatorStr;
    }

    @Override
    public boolean compare(Node other) {
        if (other != null && other instanceof AssignOperator) {
            return _operatorStr.equals(((AssignOperator) other)._operatorStr);
        }
        return false;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_operatorStr);
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(_operatorStr);
    }

}
