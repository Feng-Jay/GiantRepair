
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.ExprList;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SuperConstructorInv extends Stmt {

	private static final long serialVersionUID = -6063679105312839664L;
	private Expr _expression = null;
	private MType _superType = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperConstructorInvocation:
     *	[ Expression . ]
     *	    [ < Type { , Type } > ]
     *	    super ( [ Expression { , Expression } ] ) ;
	 */
	public SuperConstructorInv(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public SuperConstructorInv(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SCONSTRUCTORINV;
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setSuperType(MType type){
		_superType = type;
	}
	
	public void setArguments(ExprList arguments){
		_arguments = arguments;
	}

	public Expr getExpression() { return _expression; }

	public MType getSuperType() { return _superType; }

	public ExprList getArgument() { return _arguments; }

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if(_expression != null){
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(");");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_expression != null){
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		if(_expression != null) {
			children.add(_expression);
		}
		children.add(_arguments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof SuperConstructorInv) {
			SuperConstructorInv superConstructorInv = (SuperConstructorInv) other;
			if(_expression == null) {
				match = (superConstructorInv._expression == null);
			} else {
				match = _expression.compare(superConstructorInv._expression);
			}
			
//			if(_superType == null) {
//				match = match && (superConstructorInv._superType == null);
//			} else {
//				match = match && _superType.toString().equals(superConstructorInv._superType.toString());
//			}
			match = match && _arguments.compare(superConstructorInv._arguments);
		}
		return match;
	}

}
