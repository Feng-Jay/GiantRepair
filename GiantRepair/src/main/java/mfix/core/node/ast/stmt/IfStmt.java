
package mfix.core.node.ast.stmt;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class IfStmt extends Stmt {

	private static final long serialVersionUID = -7247565482784755352L;
	private Expr _condition = null;
	private Stmt _then = null;
	private Stmt _else = null;
	
	/**
	 * IfStatement:
     *	if ( Expression ) Statement [ else Statement]
	 */
	/*
	* Modification: only if-then, no else.
	*
	* */
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public IfStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.IF;
	}

	public IfStmt(IfStmt another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._condition = another.getCondition();
		this._then = another._then;
		this._else = another._else;
	}
	
	public void setCondition(Expr condition){
		_condition = condition;
	}
	
	public void setThen(Stmt then){
		_then = then;
	}
	
	public void setElse(Stmt els){
		_else = els;
	}
	
	public Expr getCondition(){
		return _condition;
	}
	
	public Stmt getThen(){
		return _then;
	}
	
	public Stmt getElse(){
		return _else;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer("if(");
		stringBuffer.append(_condition.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_then.toSrcString());
//		if(_else != null) {
//			stringBuffer.append(_else.toSrcString());
//		}
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("if");
		_tokens.add("(");
		_tokens.addAll(_condition.tokens());
		_tokens.add(")");
		_tokens.addAll(_then.tokens());
		if(_else != null) {
			_tokens.add("else");
			_tokens.addAll(_else.tokens());
		}
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_condition);
		children.add(_then);
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(2);
		if(_then != null) {
			children.add(_then);
		}
		if(_else != null) {
			children.add(_else);
		}
		return children;
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) other;
			match = _condition.compare(ifStmt._condition) && _then.compare(ifStmt._then);
			if(_else == null) {
				match = match && (ifStmt._else == null);
			} else {
				match = match && _else.compare(ifStmt._else);
			}
		}
		return match;
	}
}
