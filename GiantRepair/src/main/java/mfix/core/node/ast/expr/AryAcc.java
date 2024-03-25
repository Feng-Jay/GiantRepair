package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AryAcc extends Expr {

    private static final long serialVersionUID = 3197483700688117500L;
    private Expr _index = null;
    private Expr _array = null;

    /**
     * ArrayAccess:
     * Expression [ Expression ]
     */
    public AryAcc(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ARRACC;
    }

    public AryAcc(AryAcc another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
    }

    public void setArray(Expr array) {
        _array = array;
    }

    public Expr getArray() {
        return _array;
    }

    public void setIndex(Expr index) {
        _index = index;
    }

    public Expr getIndex() {
        return _index;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(2);
        children.add(_array);
        children.add(_index);
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_array.toSrcString());
        stringBuffer.append("[");
        stringBuffer.append(_index.toSrcString());
        stringBuffer.append("]");
        return stringBuffer;
    }

    @Override
    public void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.addAll(_array.tokens());
        _tokens.add("[");
        _tokens.addAll(_index.tokens());
        _tokens.add("]");
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof AryAcc) {
            match = _array.compare(((AryAcc) other)._array) && _index.compare(((AryAcc) other)._index);
        }
        return match;
    }

}
