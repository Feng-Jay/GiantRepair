package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FieldAcc extends Expr {

	private static final long serialVersionUID = -7480080890886474478L;
	private Expr _expression = null;
	private SName _identifier = null;
	
	
	/**
	 * FieldAccess:
     *           Expression . Identifier
	 */
	public FieldAcc(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.FIELDACC;
	}

	public FieldAcc(FieldAcc another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._expression = another._expression;
	}
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public void setIdentifier(SName identifier){
		_identifier = identifier;
	}

	public Expr getExpression() {
		return _expression;
	}

	public SName getIdentifier() {
		return _identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_expression.toSrcString());
		stringBuffer.append(".");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_expression.tokens());
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if(other != null && other instanceof FieldAcc) {
			FieldAcc fieldAcc = (FieldAcc) other;
			match = _expression.compare(fieldAcc._expression);
			match = match && _identifier.compare(fieldAcc._identifier);
		}
		return match;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>();
		children.add(_expression);
		children.add(_identifier);
		return children;
	}

	@Override
	public List<Node> flattenTreeNode(List<Node> nodes) {
		nodes.add(this);
		return nodes;
	}

}
