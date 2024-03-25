
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BreakStmt extends Stmt {

	private static final long serialVersionUID = 228415180803512647L;
	private SName _identifier = null;
	
	/**
	 * BreakStatement:
     *	break [ Identifier ] ;
	 */
	public BreakStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public BreakStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.BREACK;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("break");
		if(_identifier != null){
			stringBuffer.append(" ");
			stringBuffer.append(_identifier.toSrcString());
		}
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_identifier != null) {
			_tokens.addAll(_identifier.tokens());
		}
		_tokens.add(";");
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_identifier != null) {
			children.add(_identifier);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof BreakStmt) {
			BreakStmt breakStmt = (BreakStmt) other;
			match = _identifier == null ? (breakStmt._identifier == null) : _identifier.compare(breakStmt._identifier);
		}
		return match;
	}

}
