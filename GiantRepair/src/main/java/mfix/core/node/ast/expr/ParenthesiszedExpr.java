
package mfix.core.node.ast.expr;

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


public class ParenthesiszedExpr extends Expr {

	private static final long serialVersionUID = -8417799816148324460L;
	private Expr _expression = null;
	
	/**
	 * ParenthesizedExpression:
     *	( Expression )
	 */
	public ParenthesiszedExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.PARENTHESISZED;
	}

	public ParenthesiszedExpr(ParenthesiszedExpr another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
	}

	public void setExpr(Expr expression) {
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof ParenthesiszedExpr) {
			ParenthesiszedExpr parenthesiszedExpr = (ParenthesiszedExpr) other;
			match = _expression.compare(parenthesiszedExpr._expression);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}

}
