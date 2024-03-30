
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.List;


public abstract class Operator extends Node {

	private static final long serialVersionUID = -6189851424541008969L;

	public Operator(String fileName, int startLine, int endLine, ASTNode oriNode) {
		super(fileName, startLine, endLine, oriNode);
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
