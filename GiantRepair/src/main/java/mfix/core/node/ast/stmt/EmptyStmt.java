
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EmptyStmt extends Stmt {

	private static final long serialVersionUID = -7049209332809553395L;

	/**
	 * EmptyStatement:
     *	;
	 */
	public EmptyStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public EmptyStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(";");
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public boolean compare(Node other) {
		if(other != null && other instanceof EmptyStmt) {
			return true;
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
