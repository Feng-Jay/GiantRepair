package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Comment extends Expr {

    private static final long serialVersionUID = -6085082564199071574L;
    private String _comment;

    /**
     * Annotation:
     * NormalAnnotation
     * MarkerAnnotation
     * SingleMemberAnnotation
     */
    public Comment(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.COMMENT;
        _comment = node.toString();
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(_comment);
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add(_comment);
    }

    @Override
    public boolean compare(Node other) {
        if (other != null && other instanceof Comment) {
            return _comment.equals(((Comment) other)._comment);
        }
        return false;
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

}
