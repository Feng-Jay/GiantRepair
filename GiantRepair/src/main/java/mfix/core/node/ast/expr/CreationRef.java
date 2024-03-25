package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CreationRef extends Expr {

    private static final long serialVersionUID = 6237635456129751926L;

    private String _str;

    /**
     * CreationReference:
     *      Type ::
     *          [ < Type { , Type } > ]
     *      new
     */
    public CreationRef(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _str = node.toString();
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_str);
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(_str);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof CreationRef) {
            CreationRef creationRef = (CreationRef) other;
            match = _str.equals(creationRef.toString());
        }
        return match;
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

}
