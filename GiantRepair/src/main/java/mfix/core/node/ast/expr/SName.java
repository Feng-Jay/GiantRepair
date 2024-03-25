
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SName extends Label {

	private static final long serialVersionUID = 6548845608841663421L;
	private String _name = null;
	
	/**
	 * SimpleName:
     *	Identifier
	 */
	public SName(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SNAME;
	}

	public void setName(String name) {
		_name = name;
	}

	public String getName() {
		return _name;
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_name);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_name);
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof SName) {
			SName sName = (SName) other;
			return _name.equals(sName._name);
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
