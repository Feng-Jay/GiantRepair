
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.List;

public abstract class Expr extends Node {

    private static final long serialVersionUID = 1325289211050496258L;
    protected String _exprTypeStr = "?";
    protected transient Type _exprType = null;

    protected boolean _abstractName = true;
    protected boolean _abstractType = true;

    protected Expr(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node, null);
    }

    public void setType(Type exprType) {
        _exprType = exprType;
        if (exprType == null) {
            _exprTypeStr = "?";
        } else {
            _exprTypeStr = exprType.toString();
        }
    }

    public Type getType() {
        return _exprType;
    }

    public String getTypeString() {
        return _exprTypeStr;
    }

    @Override
    public Stmt getParentStmt() {
        return getParent().getParentStmt();
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    private boolean guarantee(Node node) {
        return NodeUtils.isMethodName(this) == NodeUtils.isMethodName(node)
                && node.getNodeType() != TYPE.VARDECLEXPR && node.getNodeType() != TYPE.SINGLEVARDECL
                && ((node.getNodeType() == TYPE.NULL) == (getNodeType() == TYPE.NULL));
    }

    @Override
    public String getTypeStr() {
        return NodeUtils.distillBasicType(_exprTypeStr);
    }
}