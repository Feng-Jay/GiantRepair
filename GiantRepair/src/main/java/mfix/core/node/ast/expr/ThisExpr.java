
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


public class ThisExpr extends Expr {

	private static final long serialVersionUID = -6319858838161670401L;

	/**
	 * ThisExpression:
     *	[ ClassName . ] this
	 */
	public ThisExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.THIS;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer("this");
	}


	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("this");
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof ThisExpr) {
			return _oriNode.toString().equals(((ThisExpr) other)._oriNode.toString());
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
