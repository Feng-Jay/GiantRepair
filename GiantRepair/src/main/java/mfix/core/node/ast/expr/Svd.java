
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class Svd extends Expr {

	private static final long serialVersionUID = 3849439897999091912L;
	private MType _decType = null;
	private SName _name = null;
	private Expr _initializer = null;
	private boolean _isVariant = false;
	
	/**
	 * { ExtendedModifier } Type {Annotation} [ ... ] Identifier { Dimension } [ = Expression ]
	 * "..." should not be appear since it is only used in method declarations
	 */
	public Svd(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.SINGLEVARDECL;
	}
	public Svd(Svd another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._decType = another._decType;
		this._name    = another._name;
		this._isVariant = another._isVariant;
	}

	public void setVariant(boolean isVariant) {
		_isVariant = isVariant;
	}

	public void setDecType(MType decType) {
		_decType = decType;
	}

	public void setName(SName name) {
		_name = name;
	}

	public MType getDeclType() {
		return _decType;
	}

	public Expr getInitializer() {
		return _initializer;
	}

	public SName getName() {
		return _name;
	}

	public void setInitializer(Expr initializer) {
		_initializer = initializer;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_decType.toSrcString());
		stringBuffer.append(_isVariant ? "... " : " ");
		stringBuffer.append(_name.toSrcString());
		if (_initializer != null) {
			stringBuffer.append("=");
			stringBuffer.append(_initializer.toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_decType.tokens());
		if (_isVariant) {
			_tokens.add("...");
		}
		_tokens.addAll(_name.tokens());
		if (_initializer != null) {
			_tokens.addFirst("=");
			_tokens.addAll(_initializer.tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof Svd) {
			Svd svd = (Svd) other;
			match = _decType.compare(svd._decType);
			match = match && _name.compare(svd._name);
			match = match && _isVariant == svd._isVariant;
			if (_initializer == null) {
				match = match && (svd._initializer == null);
			} else {
				match = match && _initializer.compare(svd._initializer);
			}
		}

		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(3);
		children.add(_decType);
		children.add(_name);
		if (_initializer != null) {
			children.add(_initializer);
		}
		return children;
	}

}
