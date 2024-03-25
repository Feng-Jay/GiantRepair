
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class WhileStmt extends Stmt {

	private static final long serialVersionUID = -5763865331484587703L;
	private Expr _expression = null;
	private Stmt _body = null;
	
	/**
	 * WhileStatement:
     *	while ( Expression ) Statement
	 */
	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public WhileStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.WHILE;
	}

	public WhileStmt(WhileStmt another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
	}
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}
	
	public void setBody(Stmt body){
		_body = body;
	}
	
	public Stmt getBody() {
		return _body;
	}
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_body.toSrcString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
		_tokens.addAll(_body.tokens());
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_expression);
		children.add(_body);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_body);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) other;
			match = _expression.compare(whileStmt._expression) && _body.compare(whileStmt._body);
		}
		return match;
	}

}

