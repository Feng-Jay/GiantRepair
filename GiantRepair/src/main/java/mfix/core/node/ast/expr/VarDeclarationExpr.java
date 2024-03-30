
package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class VarDeclarationExpr extends Expr {

	private static final long serialVersionUID = -5908284718888454712L;
	private MType _declType = null;
	private List<Vdf> _vdfs = null;


	/**
	 * VariableDeclarationExpression:
     *	{ ExtendedModifier } Type VariableDeclarationFragment
     *	    { , VariableDeclarationFragment }
	 */
	public VarDeclarationExpr(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.VARDECLEXPR;
	}

	public VarDeclarationExpr(VarDeclarationExpr another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._declType = another._declType;
	}

	public void setDeclType(MType declType) {
		_declType = declType;
	}

	public void setVarDeclFrags(List<Vdf> vdfs) {
		_vdfs = vdfs;
	}

	public MType getDeclType() {
		return _declType;
	}

	public List<Vdf> getFragments() {
		return _vdfs;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_declType.toSrcString());
		stringBuffer.append(" ");
		stringBuffer.append(_vdfs.get(0).toSrcString());
		for (int i = 1; i < _vdfs.size(); i++) {
			stringBuffer.append(",");
			stringBuffer.append(_vdfs.get(i).toSrcString());
		}
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.addAll(_declType.tokens());
		_tokens.addAll(_vdfs.get(0).tokens());
		for (int i = 1; i < _vdfs.size(); i++) {
			_tokens.add(",");
			_tokens.addAll(_vdfs.get(i).tokens());
		}
	}

	@Override
	public boolean compare(Node other) {
		boolean match = false;
		if (other != null && other instanceof VarDeclarationExpr) {
			VarDeclarationExpr varDeclarationExpr = (VarDeclarationExpr) other;
			match = _declType.compare(varDeclarationExpr._declType);
			match = match && (_vdfs.size() == varDeclarationExpr._vdfs.size());
			for (int i = 0; match && i < _vdfs.size(); i++) {
				match = match && _vdfs.get(i).compare(varDeclarationExpr._vdfs.get(i));
			}
		}
		return match;
	}

	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(_vdfs.size() + 1);
		children.add(_declType);
		children.addAll(_vdfs);
		return children;
	}

}
