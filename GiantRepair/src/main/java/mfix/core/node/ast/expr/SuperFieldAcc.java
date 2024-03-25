
package mfix.core.node.ast.expr;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SuperFieldAcc extends Expr {

	private static final long serialVersionUID = 1921879022776437618L;
	private Label _name = null;
	private SName _identifier = null;
	
	/**
	 * SuperFieldAccess:
     *	[ ClassName . ] super . Identifier
	 */
	public SuperFieldAcc(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SFIELDACC;
	}

	public SuperFieldAcc(SuperFieldAcc another){
		this(null, 0, 0, null);
		this._name = another._name;
	}

	public void setName(Label name) {
		_name = name;
	}

	public void setIdentifier(SName identifier) {
		_identifier = identifier;
	}

	public SName getIdentifier() {
		return _identifier;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_name != null) {
			stringBuffer.append(_name.toSrcString());
			stringBuffer.append(".");
		}
		stringBuffer.append("super.");
		stringBuffer.append(_identifier.toSrcString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_name != null) {
			_tokens.addAll(_name.tokens());
			_tokens.add(".");
		}
		_tokens.add("super");
		_tokens.add(".");
		_tokens.addAll(_identifier.tokens());
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof SuperFieldAcc) {
			SuperFieldAcc superFieldAcc = (SuperFieldAcc) other;
			match = (_name == null) ? (superFieldAcc._name == null) : _name.compare(superFieldAcc._name);
			if (match) {
				match = match && _identifier.compare(superFieldAcc._identifier);
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		if (_name != null) {
			children.add(_name);
		}
		children.add(_identifier);
		return children;
	}

	public List<Node> flattenTreeNode(List<Node> nodes) {
		nodes.add(this);
		return nodes;
	}

}
