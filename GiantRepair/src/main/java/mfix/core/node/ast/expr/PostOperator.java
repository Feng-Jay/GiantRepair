
package mfix.core.node.ast.expr;

import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.PostfixExpression;

import java.util.LinkedList;
import java.util.Map;


public class PostOperator extends Operator {

	private static final long serialVersionUID = -487330256404513705L;
	private String _operatorStr;
	private transient PostfixExpression.Operator _operator;

	public PostOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.POSTOPERATOR;
	}

	public void setOperator(PostfixExpression.Operator operator) {
		this._operator = operator;
		this._operatorStr = operator.toString();
	}

	public PostfixExpression.Operator getOperator() {
		return _operator;
	}

	public String getOperatorStr() {
		return _operatorStr;
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof PostOperator) {
			return _operatorStr.equals(((PostOperator) other)._operatorStr);
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_operatorStr);
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_operatorStr);
	}

}
