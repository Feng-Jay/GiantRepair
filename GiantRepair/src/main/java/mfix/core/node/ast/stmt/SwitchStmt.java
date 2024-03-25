
package mfix.core.node.ast.stmt;

import mfix.common.conf.Constant;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SwitchStmt extends Stmt {

	private static final long serialVersionUID = 242211567501322520L;
	private Expr _expression = null;
	private List<Stmt> _statements = null;
	
	/**
	 * SwitchStatement:
     *           switch ( Expression )
     *                   { { SwitchCase | Statement } }
 	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwitchStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public SwitchStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SWSTMT;
	}

	public SwitchStmt(SwitchStmt another){
		this(another._fileName, another._startLine, another._endLine, another.getOriNode(), null);
		this._statements = new ArrayList<>();
	}

	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setStatements(List<Stmt> statements){
		_statements = statements;
	}

	public Expr getExpression() { return _expression; }

	public List<Stmt> getStatements() { return _statements; }
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("switch (");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append("){" + Constant.NEW_LINE);
		for (Stmt stmt : _statements) {
			stringBuffer.append(stmt.toSrcString());
			stringBuffer.append(Constant.NEW_LINE);
		}
		stringBuffer.append("}");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("switch");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.add("{");
		for(Stmt stmt : _statements) {
			_tokens.addAll(stmt.tokens());
		}
		_tokens.add("}");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_statements.size() + 1);
		children.add(_expression);
		children.addAll(_statements);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return _statements;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) other;
			match = _expression.compare(switchStmt._expression)
					&& (_statements.size() == switchStmt._statements.size());
			for (int i = 0; match && i < _statements.size(); i++) {
				match = match && _statements.get(i).compare(switchStmt._statements.get(i));
			}
		}
		return match;
	}
}
