
package mfix.core.node.ast.expr;

import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.LinkedList;
import java.util.Map;


public class InfixOperator extends Operator {

	private static final long serialVersionUID = -4702533056648468078L;
	private String _operatorStr;
	private transient InfixExpression.Operator _operator;

	public InfixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.INFIXOPERATOR;
	}

	public void setOperator(InfixExpression.Operator operator) {
		_operator = operator;
		_operatorStr = operator.toString();
	}
	public void setOperatorStr(String operatorStr) {
		_operatorStr = operatorStr;
	}

	public InfixExpression.Operator getOperator() {
		return _operator;
	}

	public String getOperatorStr() {
		return _operatorStr;
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof InfixOperator) {
			InfixOperator infixOperator = (InfixOperator) other;
			return _operatorStr.equals(infixOperator._operatorStr);
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
