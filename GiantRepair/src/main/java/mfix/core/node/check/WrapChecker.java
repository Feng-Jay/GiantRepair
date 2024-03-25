package mfix.core.node.check;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.PrefixExpr;
import mfix.core.node.ast.expr.PrefixOperator;
import mfix.core.node.ast.stmt.IfStmt;

import java.util.ArrayList;
import java.util.List;

public class WrapChecker {
    private final CheckParser _checkParser;

    public WrapChecker(CheckParser checkParser) {
        _checkParser = checkParser;
    }

    public List<Node> check(IfStmt node){
        Expr expr = node.getCondition();
        List<Node> candidates = _checkParser.process(expr, "Boolean");
        List<Node> ret = new ArrayList<>();
        for(Node candidate: candidates){
            IfStmt ifStmt = new IfStmt(node);
            ifStmt.setCondition((Expr) candidate);

            PrefixExpr prefixExpr = new PrefixExpr(expr.getFileName(), expr.getStartLine(), expr.getEndLine(), expr.getOriNode());
            prefixExpr.setExpression((Expr) candidate);
            PrefixOperator prefixOperator = new PrefixOperator(expr.getFileName(), expr.getStartLine(), expr.getEndLine(), expr.getOriNode());
            prefixOperator.setOperatorStr("!");
            prefixExpr.setOperator(prefixOperator);
            IfStmt elseIfStmt = new IfStmt(node);
            elseIfStmt.setCondition(prefixExpr);
            ret.add(ifStmt);
            ret.add(elseIfStmt);
        }
        return ret;
    }

    public List<Node> process(Node node){
        if(node instanceof IfStmt){
           return check((IfStmt) node);
        }else{
            List<Node> ret = new ArrayList<>();
            ret.add(node);
            return ret;
        }
    }
}
