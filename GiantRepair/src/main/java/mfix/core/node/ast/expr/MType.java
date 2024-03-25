
package mfix.core.node.ast.expr;

import mfix.core.node.NodeUtils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MType extends Node {

	private static final long serialVersionUID = 1247523997810234312L;
	private String _typeStr;
	private transient Type _type;

	public MType(String fileName, int startLine, int endLine, Type oriNode) {
		super(fileName, startLine, endLine, oriNode);
		_nodeType = TYPE.TYPE;
		_type = oriNode;
		_typeStr = oriNode.toString();
	}

	public MType(MType another){
		super(another._fileName, another._startLine, another._endLine, another._oriNode);
		this._nodeType = TYPE.TYPE;
		this._type = another.type();
	}

	public void setTypeStr(String str){
		_typeStr = str;
	}

	public void setType(Type type) {
		if (type == null) {
			type = AST.newAST(AST.JLS8).newWildcardType();
		}
		if (_type == null) {
			this._type = type;
			_typeStr = type.toString();
		}
	}

	public Type type() {
		return _type;
	}

	public boolean isArrayType() {
		return _type.isArrayType();
	}

	public String typeStr() {
		return _typeStr;
	}

	public Type getElementType() {
		if (isArrayType()) {
			return ((ArrayType) _type).getElementType();
		} else {
			return _type;
		}
	}

	@Override
	public String getTypeStr() {
		return NodeUtils.distillBasicType(this);
	}

	public int getDimension() {
		if (isArrayType()) {
			return ((ArrayType) _type).getDimensions();
		} else {
			return 0;
		}
	}

	@Override
	public boolean compare(Node other) {
		if (other != null && other instanceof MType) {
			return _typeStr.equals(((MType) other)._typeStr);
		}
		return false;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append(_typeStr);
		return stringBuffer;
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_typeStr);
	}

//	@Override
//	public boolean equals(Object obj) {
//		if (obj == null || !(obj instanceof MType)) {
//			return false;
//		}
//		MType mType = (MType) obj;
//		return _typeStr.equals(mType._typeStr);
//	}

}
