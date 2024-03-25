
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.*;


public class SuperMethodInv extends Expr {

	private static final long serialVersionUID = -227589196009347171L;
	private Label _label = null;
	private SName _name = null;
	private ExprList _arguments = null;
	
	/**
	 * SuperMethodInvocation:
     *	[ ClassName . ] super .
     *    [ < Type { , Type } > ]
     *    Identifier ( [ Expression { , Expression } ] )
	 */
	public SuperMethodInv(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SMINVOCATION;
	}

	public SuperMethodInv(SuperMethodInv another){
		this(null, 0, 0, null);
		this._label = another._label;
	}

	public void setLabel(Label label) {
		_label = label;
	}

	public void setName(SName name) {
		_name = name;
	}

	public void setArguments(ExprList arguments) {
		_arguments = arguments;
	}

	public SName getMethodName() {
		return _name;
	}

	public ExprList getArguments() {
		return _arguments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_label != null) {
			stringBuffer.append(_label.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_name.toSrcString());
		stringBuffer.append("(");
		stringBuffer.append(_arguments.toSrcString());
		stringBuffer.append(")");
		return stringBuffer;
	}

	@Override
	public Set<SName> getAllVars() {
		Set<SName> set = new HashSet<>();
		if (_label != null) {
			set.addAll(_label.getAllVars());
		}
		set.addAll(_arguments.getAllVars());
		return set;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_label != null) {
			_tokens.addAll(_label.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_name.tokens());
		_tokens.add("(");
		_tokens.addAll(_arguments.tokens());
		_tokens.add(")");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof SuperMethodInv) {
			SuperMethodInv superMethodInv = (SuperMethodInv) other;
			match = (_label == null) ? (superMethodInv._label == null) : _label.compare(superMethodInv._label);
			match = match && _name.compare(superMethodInv._name) && _arguments.compare(superMethodInv._arguments);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		if (_label != null) {
			children.add(_label);
		}
		children.add(_name);
		children.add(_arguments);
		return children;
	}

	public List<Node> flattenTreeNode(List<Node> nodes) {
		nodes.add(this);
		_arguments.flattenTreeNode(nodes);
		return nodes;
	}

}
