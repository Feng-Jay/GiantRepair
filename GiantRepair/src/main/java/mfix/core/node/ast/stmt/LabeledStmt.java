
package mfix.core.node.ast.stmt;

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


public class LabeledStmt extends Stmt {

	private static final long serialVersionUID = -2484110665315045636L;
	private String _codeStr;
	/**
	 * LabeledStatement:
     *	Identifier : Statement
	 */
	public LabeledStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public LabeledStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_codeStr = node.toString();
	}
	
	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_codeStr);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
	}
	
	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof LabeledStmt) {
			match = _codeStr.equals(((LabeledStmt) other)._codeStr);
		}
		return match;
	}
}
