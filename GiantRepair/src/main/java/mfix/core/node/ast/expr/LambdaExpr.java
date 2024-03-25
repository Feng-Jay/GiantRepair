
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


public class LambdaExpr extends Expr {

	private static final long serialVersionUID = -7877574560065780171L;

	private String _codeStr;
	//TODO : add support for lambda expression

	/**
	 * LambdaExpression:
     *	Identifier -> Body
     *	( [ Identifier { , Identifier } ] ) -> Body
     *	( [ FormalParameter { , FormalParameter } ] ) -> Body
	 */
	public LambdaExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_codeStr = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_codeStr);
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_codeStr);
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof LambdaExpr) {
			match = _codeStr.equals(((LambdaExpr) other)._codeStr);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}

