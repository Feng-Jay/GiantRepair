
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;


public class LongLiteral extends NumLiteral {

	private static final long serialVersionUID = -5464691868940145050L;
	private long _value = 0L;

	public LongLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.LLITERAL;
	}

	public void setValue(long value) {
		_value = value;
	}

	public long getValue() {
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
		if (other instanceof LongLiteral) {
			LongLiteral literal = (LongLiteral) other;
			match = (_value == literal._value);
		}
		return match;
	}
	
}
