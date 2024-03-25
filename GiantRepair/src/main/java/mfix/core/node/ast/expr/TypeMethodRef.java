
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


public class TypeMethodRef extends Expr {

	private static final long serialVersionUID = 7592430157234480970L;

	/**
	 * TypeMethodReference:
     *	Type :: 
     *	    [ < Type { , Type } > ]
     *	    Identifier
	 */
	public TypeMethodRef(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_oriNode.toString());
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_oriNode.toString());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof TypeMethodRef) {
			match = _oriNode.toString().equals(((TypeMethodRef) other)._oriNode.toString());
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
