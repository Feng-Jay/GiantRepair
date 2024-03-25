
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


public class DoStmt extends Stmt {

	private static final long serialVersionUID = -8707533085331564948L;
	private Stmt _stmt = null;
	private Expr _expression = null;
	
	/**
	 * DoStatement:
     *	do Statement while ( Expression ) ;
	 */
	public DoStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public DoStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.DO;
	}

	public DoStmt(DoStmt another){
		this(another._fileName, another._startLine, another._endLine, another.getOriNode());
	}
	
	public void setBody(Stmt stmt){
		_stmt = stmt;
	}
	
	public Stmt getBody() {
		return _stmt;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("do ");
		stringBuffer.append(_stmt.toSrcString());
		stringBuffer.append(" while(");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("do");
		_tokens.addAll(_stmt.tokens());
		_tokens.add("while");
		_tokens.add("(");
		_tokens.addAll(_expression.tokens());
		_tokens.add(")");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_stmt);
		children.add(_expression);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		if(_stmt != null) {
			children.add(_stmt);
		}
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof DoStmt) {
			DoStmt doStmt = (DoStmt) other;
			match = _expression.compare(doStmt._expression) && _stmt.compare(doStmt._stmt);
		}
		return match;
	}

}
