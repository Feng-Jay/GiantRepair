package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ElseStmt extends Stmt{
    private static final long serialVersionUID = -7247565482784755353L;

    private Stmt _stmt = null;

    public ElseStmt(String fileName, int startLine, int endLine, ASTNode node){
        this(fileName, startLine, endLine, node, null);
    }

    public ElseStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent){
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.ELSE;
    }

    public ElseStmt(ElseStmt another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
        this._stmt = another._stmt;
    }

    public void setBody(Stmt stmt){ _stmt = stmt;}

    public Stmt getBody(){return _stmt;}

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("else");
        stringBuffer.append(_stmt.toSrcString());
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("else");
        _tokens.addAll(_stmt.tokens());
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(3);
        children.add(_stmt);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        List<Stmt> children = new ArrayList<>(2);
        children.add(_stmt);
        return children;
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if(other != null && other instanceof ElseStmt)
            match = _stmt.compare(((ElseStmt) other)._stmt);
        return match;
    }

}
