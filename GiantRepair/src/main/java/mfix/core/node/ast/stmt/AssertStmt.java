
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


public class AssertStmt extends Stmt {

	private static final long serialVersionUID = 8494694375316529776L;
	private Expr _expresion;
	private Expr _message;
	// TODO: assert statement should be added
	/**
	 * AssertStatement:
     *	assert Expression [ : Expression ] ;
	 */
	public AssertStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public AssertStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.ASSERT;
	}

	public AssertStmt(AssertStmt another){
		this(another.getFileName(), another._startLine, another.getEndLine(), another.getOriNode(), null);
		this._message   = another.getMessage();
	}

	public void setExpression(Expr expression) {
		_expresion = expression;
	}

	public Expr getExpression() {
		return _expresion;
	}

	public void setMessage(Expr message) {
		_message = message;
	}

	public Expr getMessage() {
		return _message;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("assert ")
				.append(_expresion.toSrcString());
		if (_message != null) {
			buffer.append(":").append(_message.toSrcString());
		}
		buffer.append(";");
		return buffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("assert");
		_tokens.addAll(_expresion.tokens());
		if (_message != null) {
			_tokens.addAll(_message.tokens());
		}
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof AssertStmt) {
			AssertStmt assertStmt = (AssertStmt) other;
			if (_expresion.compare(assertStmt.getExpression())) {
				if (_message == null) {
					return assertStmt.getMessage() == null;
				} else {
					return _message.compare(assertStmt.getMessage());
				}
			}
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> nodes = new ArrayList<>(2);
		nodes.add(_expresion);
		if (_message != null) {
			nodes.add(_message);
		}
		return nodes;
	}

}