
package mfix.core.node.ast.expr;

import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class NillLiteral extends Expr {

	private static final long serialVersionUID = 3790017124979289916L;
	private String _value = "null";
	
	/**
	 * Null literal node.
	 */
	public NillLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.NULL;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_value);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("null");
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof NillLiteral) {
			match = true;
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
