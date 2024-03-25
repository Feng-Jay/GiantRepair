package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExprList extends Node {

    private static final long serialVersionUID = -1155629329446419826L;
    private List<Expr> _exprs;

    public ExprList(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        _nodeType = TYPE.EXPRLST;
    }

//    public ExprList(List<Expr> exprs){
//        this(null, 0, 0, null);
//        this._exprs = exprs;
//    }
    public ExprList(ExprList another){
        this(another.getFileName(), another.getStartLine(), another.getEndLine(), another.getOriNode());
        this._exprs = new ArrayList<>();
    }

    public void setExprs(List<Expr> exprs) {
        this._exprs = exprs;
    }

    public List<Expr> getExpr() {
        return _exprs;
    }

    @Override
    public boolean compare(Node other) {
        if (other != null && other instanceof ExprList) {
            ExprList exprList = (ExprList) other;
            if (_exprs.size() == exprList._exprs.size()) {
                for (int i = 0; i < _exprs.size(); i++) {
                    if (!_exprs.get(i).compare(exprList._exprs.get(i))) {
                        return false;
                    }
                }
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (_exprs.size() > 0) {
            stringBuffer.append(_exprs.get(0).toSrcString());
            for (int i = 1; i < _exprs.size(); i++) {
                stringBuffer.append(",");
                stringBuffer.append(_exprs.get(i).toSrcString());
            }
        }
        return stringBuffer;
    }


    @Override
    public Stmt getParentStmt() {
        return getParent().getParentStmt();
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_exprs.size());
        children.addAll(_exprs);
        return children;
    }

    @Override
    public List<Stmt> getChildren() {
        return new ArrayList<>(0);
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        if (_exprs.size() > 0) {
            _tokens.addAll(_exprs.get(0).tokens());
            for (int i = 1; i < _exprs.size(); i++) {
                _tokens.add(",");
                _tokens.addAll(_exprs.get(i).tokens());
            }
        }
    }

}
