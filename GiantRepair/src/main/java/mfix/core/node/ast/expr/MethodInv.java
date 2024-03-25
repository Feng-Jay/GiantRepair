
package mfix.core.node.ast.expr;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MethodInv extends Expr {

	private static final long serialVersionUID = 3902854514191993113L;
	private Expr _expression = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 *  MethodInvocation:
     *  [ Expression . ]
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public MethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.MINVOCATION;
	}

	public MethodInv(MethodInv another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._expression = another._expression;
		this._name = another._name;
		this._arguments = another._arguments;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public void setName(SName name) {
		_name = name;
	}

	public void setArguments(ExprList arguments) {
		_arguments = arguments;
	}

	public Expr getExpression() {
		return _expression;
	}

	public SName getName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression != null) {
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		if (_arguments != null) {
			stringBuffer.append(_arguments.toSrcString());
		}
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_expression != null) {
			set.addAll(_expression.getAllVars());
		}
		if (_arguments != null) {
			set.addAll(_arguments.getAllVars());
		}
		return set;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_expression != null) {
			_tokens.addAll(_expression.tokens());
			_tokens.add(".");
		}
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		if (_arguments != null) {
			_tokens.addAll(_arguments.tokens());
		}
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof MethodInv) {
			MethodInv methodInv = (MethodInv) other;
			match = _name.compare(methodInv._name);
			if (match) {
				match = (_expression == null ? (methodInv._expression == null)
						: _expression.compare(methodInv._expression)) && _arguments.compare(methodInv._arguments);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if (_expression != null) {
			children.add(_expression);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}

}
