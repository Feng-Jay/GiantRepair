
package mfix.core.node.ast.stmt;

import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.Map;


public abstract class Stmt extends Node {

	private static final long serialVersionUID = -4168850816999087148L;

	protected Stmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
	}

//	public boolean equals(Node other){
//		return this.getStartLine() == other.getStartLine() && this.getNodeType()==other.getNodeType();
//	}

	@Override
	public Stmt getParentStmt() {
		return this;
	}

}