
package mfix.core.node.ast.stmt;

import mfix.common.util.Utils;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.MType;
import mfix.core.node.ast.expr.Vdf;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.match.Matcher;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class VarDeclarationStmt extends Stmt {

	private static final long serialVersionUID = 3322205918544098505L;
	private String _modifier = null;
	private MType _declType = null;
	private List<Vdf> _fragments = null;

	/**
	 * VariableDeclarationStatement: { ExtendedModifier } Type
	 * VariableDeclarationFragment { , VariableDeclarationFragment } ;
	 */
	public VarDeclarationStmt(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}

	public VarDeclarationStmt(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.VARDECLSTMT;
	}

	public VarDeclarationStmt(VarDeclarationStmt another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
		this._modifier = another._modifier;
		this._declType = another._declType;
	}

	/**
	 * @param modifier
	 *            the modifier to set
	 */
	public void setModifier(String modifier) {
		this._modifier = modifier;
	}

	public void setDeclType(MType declType) {
		_declType = declType;
	}

	public void setFragments(List<Vdf> fragments) {
		_fragments = fragments;
	}

	public String getModifier() {
		return _modifier;
	}

	public MType getDeclType() {
		return _declType;
	}

	public List<Vdf> getFragments() {
		return _fragments;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_modifier != null) {
			stringBuffer.append(_modifier).append(' ');
		}
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(' ');
		stringBuffer.append(_fragments.get(0).toSrcString());
		for (int i = 1; i < _fragments.size(); i++) {
			stringBuffer.append(',');
			stringBuffer.append(_fragments.get(i).toSrcString());
		}
		stringBuffer.append(';');
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if (_modifier != null) {
			_tokens.add(_modifier);
		}
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_fragments.get(0).tokens());
		for (int i = 1; i < _fragments.size(); i++) {
			_tokens.add(",");
			_tokens.addAll(_fragments.get(i).tokens());
		}
		_tokens.add(";");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_fragments.size() + 1);
		children.add(_declType);
		children.addAll(_fragments);
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof VarDeclarationStmt) {
			VarDeclarationStmt varDeclarationStmt = (VarDeclarationStmt) other;
			match = _declType.compare(varDeclarationStmt._declType);
			if (_modifier == null) {
				match = match && (varDeclarationStmt._modifier == null);
			} else {
				if (varDeclarationStmt._modifier == null) {
					match = false;
				} else {
					match = match && _modifier.equals(varDeclarationStmt._modifier);
				}
			}
			match = match && (_fragments.size() == varDeclarationStmt._fragments.size());
			for (int i = 0; match && i < _fragments.size(); i++) {
				match = match && _fragments.get(i).compare(varDeclarationStmt._fragments.get(i));
			}
		}
		return match;
	}

}
