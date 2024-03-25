
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;


public class IntLiteral extends NumLiteral {

	private static final long serialVersionUID = 5166876752215736559L;
	private int _value = 0;

	public IntLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.INTLITERAL;
	}

	public void setValue(int value) {
		_value = value;
	}

	public int getValue() {
		return _value;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(String.valueOf(_value));
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(String.valueOf(_value));
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof IntLiteral) {
			IntLiteral intLiteral = (IntLiteral) other;
			match = (_value == intLiteral._value);
		}
		return match;
	}
	
}
