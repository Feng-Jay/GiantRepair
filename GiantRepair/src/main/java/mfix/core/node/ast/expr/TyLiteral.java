
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.abs.CodeAbstraction;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TyLiteral extends Expr {

	private static final long serialVersionUID = 5518643646465944075L;
	private MType _type = null;
	
	/**
	 * TypeLiteral:
     *	( Type | void ) . class
	 */
	public TyLiteral(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.TLITERAL;
	}

	public void setValue(MType type) {
		_type = type;
	}

	public MType getDeclType() {
		return _type;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_type.toSrcString());
		stringBuffer.append(".class");
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_type.tokens());
		_tokens.add(".");
		_tokens.add("class");
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof TyLiteral) {
			TyLiteral literal = (TyLiteral) other;
			match = _type.compare(literal._type);
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public String getTypeStr() {
		return _type.getTypeStr();
	}

}
