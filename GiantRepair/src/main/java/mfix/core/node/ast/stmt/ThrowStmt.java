
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.ClassInstCreation;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ThrowStmt extends Stmt {

	private static final long serialVersionUID = 6373618160322079237L;
	private Expr _expression = null;
	
	/**
	 * ThrowStatement:
     *	throw Expression ;
	 */
	public ThrowStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public ThrowStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.THROW;
	}

	public ThrowStmt(ThrowStmt another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}

	public Expr getExpression() { return _expression; }

	public String getExceptionType(){
		if(_expression instanceof ClassInstCreation){
			return ((ClassInstCreation)_expression).getClassType().toString();
		} else {
			return _expression.getType().toString();
		}
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("throw ");
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(";");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("throw");
		_tokens.addAll(_expression.tokens());
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		children.add(_expression);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof ThrowStmt) {
			ThrowStmt throwStmt = (ThrowStmt) other;
			match = _expression.compare(throwStmt._expression);
		}
		return match;
	}
	
}
