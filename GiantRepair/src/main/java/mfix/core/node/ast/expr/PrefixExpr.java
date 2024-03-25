
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PrefixExpr extends Expr {

	private static final long serialVersionUID = 6945905157362942363L;
	private Expr _expression = null;
	private PrefixOperator _operator = null;
	
	/**
	 * PrefixExpression:
     *	PrefixOperator Expression
	 */
	public PrefixExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PREEXPR;
	}

	public PrefixExpr(PrefixExpr another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._operator = another._operator;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public void setOperator(PrefixOperator operator) {
		_operator = operator;
	}

	public Expr getExpression() {
		return _expression;
	}

	public PrefixOperator getOperator() {
		return _operator;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operator.toSrcString());
		stringBuffer.append(_expression.toSrcString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_operator.tokens());
		_tokens.addAll(_expression.tokens());
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof PrefixExpr) {
			PrefixExpr prefixExpr = (PrefixExpr) other;
			return _operator.compare(prefixExpr._operator) && _expression.compare(prefixExpr._expression);
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_operator);
		return children;
	}

}
