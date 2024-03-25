
package mfix.core.node.ast.stmt;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class AnonymousClassDecl extends Node {

	private String _codeStr;

	private static final long serialVersionUID = -5993526474963543721L;

	public AnonymousClassDecl(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
		_nodeType = TYPE.ANONYMOUSCDECL;
		_codeStr = node.toString();
	}

	@Override
	public StringBuffer toSrcString() {
		return new StringBuffer(_codeStr);
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add(_codeStr);
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
	public boolean compare(Node other) {
		if (other != null && other instanceof AnonymousClassDecl) {
//			return _codeStr.equals(((AnonymousClassDecl) other)._codeStr);
			return true;
		}
		return false;
	}

	@Override
	public List<Node> getAllChildren() {
		return new ArrayList<>(0);
	}

}
