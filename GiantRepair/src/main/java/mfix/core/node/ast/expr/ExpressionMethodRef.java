package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExpressionMethodRef extends Expr {

	private static final long serialVersionUID = -7935543365316676426L;
	private String _str;
	/**
	 * ExpressionMethodReference:
     *	Expression :: 
     *	    [ < Type { , Type } > ]
     *	    Identifier
	 */
	public ExpressionMethodRef(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_str = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_str);
		return stringBuffer;
	}


	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_str);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof ExpressionMethodRef) {
			match = _str.equals(((ExpressionMethodRef) other)._str);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
