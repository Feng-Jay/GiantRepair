
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MethodRef extends Expr {

	private static final long serialVersionUID = -6933267554937901141L;
	private final String _str;
	/**
	 * MethodReference:
     *	CreationReference
     *	ExpressionMethodReference
     *	SuperMethodReference
     *	TypeMethodReference
	 */
	public MethodRef(String fileName, int startLine, int endLine, ASTNode node) {
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
		if (other != null && other instanceof MethodRef) {
			match = _str.equals(((MethodRef) other)._str);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
