
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ContinueStmt extends Stmt {

    private static final long serialVersionUID = -4634975771051671527L;
    private SName _identifier = null;

    /**
     * ContinueStatement:
     * continue [ Identifier ] ;
     */
    public ContinueStmt(String fileName, int startLine, int endLine, ASTNode node) {
        this(fileName, startLine, endLine, node, null);
    }

    public ContinueStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
        super(fileName, startLine, endLine, node, parent);
        _nodeType = TYPE.CONTINUE;
    }

    public void setIdentifier(SName identifier) {
        _identifier = identifier;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer("continue");
        if (_identifier != null) {
            stringBuffer.append(" ");
            stringBuffer.append(_identifier.toSrcString());
        }
        stringBuffer.append(";");
        return stringBuffer;
    }

    protected void tokenize() {
        _tokens = new LinkedList<>();
        _tokens.add("continue");
        if (_identifier != null) {
            _tokens.addAll(_identifier.tokens());
        }
        _tokens.add(";");
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(1);
        if (_identifier != null) {
            children.add(_identifier);
        }
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof ContinueStmt) {
            ContinueStmt continueStmt = (ContinueStmt) other;
            match = (_identifier == null) ? (continueStmt._identifier == null)
                    : _identifier.compare(continueStmt._identifier);
        }
        return match;
    }

}
