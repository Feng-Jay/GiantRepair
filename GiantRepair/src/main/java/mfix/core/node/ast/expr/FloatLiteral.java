package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.LinkedList;

public class FloatLiteral extends NumLiteral {

	private static final long serialVersionUID = -6015309331641968237L;
	private float _value = .0f;
	private final double EPSILON = 1e-5;

	public FloatLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.FLITERAL;
	}

	public void setValue(float value) {
		_value = value;
	}

	public float getValue() {
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
		if (other != null && other instanceof FloatLiteral) {
			return (Math.abs(_value - ((FloatLiteral) other)._value) < EPSILON);
		}
		return false;
	}
	
}
