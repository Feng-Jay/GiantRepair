
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


public class NumLiteral extends Expr {

	private static final long serialVersionUID = -8592908830390293970L;
	private String _token = null;
	
	/**
	 * Null literal node.
	 */
	public NumLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.NUMBER;
	}

	public void setValue(String token) {
		_token = token;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_token);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_token);
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof NumLiteral) {
			NumLiteral numLiteral = (NumLiteral) other;
			match = _token.equals(numLiteral._token);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
