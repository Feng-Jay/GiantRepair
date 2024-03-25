
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Vdf extends Node {

	private static final long serialVersionUID = -1445761649599489420L;
	private MType _type = null;
	private SName _identifier = null;
	private int _dimensions = 0; 
	private Expr _expression = null;
	
	/**
	 * VariableDeclarationFragment:
     *	Identifier { Dimension } [ = Expression ]
	 */
	public Vdf(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public Vdf(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.VARDECLFRAG;
	}

	public Vdf(Vdf another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
		this._type = another._type;
		this._identifier = another._identifier;
		this._dimensions = another._dimensions;
	}

	public void setName(SName identifier) {
		_identifier = identifier;
	}

	public SName getNameNode() {
		return _identifier;
	}

	public String getName() {
		return _identifier.getName();
	}

	public void setType(MType type) {
		_type = type;
	}

	public MType getType() {
		return _type;
	}

	public void setDimensions(int dimensions) {
		_dimensions = dimensions;
	}

	public int getDimension() {
		return _dimensions;
	}

	public void setExpression(Expr expression) {
		_expression = expression;
	}

	public Expr getExpression() {
		return _expression;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_identifier.toSrcString());
		for (int i = 0; i < _dimensions; i++) {
			stringBuffer.append("[]");
		}
		if (_expression != null) {
			stringBuffer.append("=");
			stringBuffer.append(_expression.toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_identifier.tokens());
		for (int i = 0; i < _dimensions; i++) {
			_tokens.add("[]");
		}
		if (_expression != null) {
			_tokens.add("=");
			_tokens.addAll(_expression.tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof Vdf) {
			Vdf vdf = (Vdf) other;
			match = (_dimensions == vdf._dimensions) && _identifier.compare(vdf._identifier);
			if (_expression == null) {
				match = match && (vdf._expression == null);
			} else {
				match = match && _expression.compare(vdf._expression);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_identifier);
		if (_expression != null) {
			children.add(_expression);
		}
		return children;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

}
