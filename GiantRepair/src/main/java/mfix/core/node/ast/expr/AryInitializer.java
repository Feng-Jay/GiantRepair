package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AryInitializer extends Expr {

    private static final long serialVersionUID = 5694794734726396689L;
    private List<Expr> _expressions = null;

    /**
     * ArrayInitializer:
     *      { [ Expression { , Expression} [ , ]] }
     */
    public AryInitializer(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.ARRINIT;
    }

    public AryInitializer(AryInitializer another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
    }

    public void setExpressions(List<Expr> expressions) {
        _expressions = expressions;
    }

    public List<Expr> getExpressions(){return _expressions;}

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_expressions.size());
        children.addAll(_expressions);
        return children;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("{");
        if (_expressions.size() > 0) {
            stringBuffer.append(_expressions.get(0).toSrcString());
            for (int i = 1; i < _expressions.size(); i++) {
                stringBuffer.append(",");
                stringBuffer.append(_expressions.get(i).toSrcString());
            }
        }
        stringBuffer.append("}");
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("{");
        if (_expressions.size() > 0) {
            _tokens.addAll(_expressions.get(0).tokens());
            for (int i = 1; i < _expressions.size(); i++) {
                _tokens.add(",");
                _tokens.addAll(_expressions.get(i).tokens());
            }
        }
        _tokens.add("}");
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof AryInitializer) {
            AryInitializer aryInitializer = (AryInitializer) other;
            match = (_expressions.size() == aryInitializer._expressions.size());
            for (int i = 0; match && i < _expressions.size(); i++) {
                match = match && _expressions.get(i).compare(aryInitializer._expressions.get(i));
            }
        }
        return match;
    }
}
