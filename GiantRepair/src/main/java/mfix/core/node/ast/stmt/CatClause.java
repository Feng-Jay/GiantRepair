
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Svd;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CatClause extends Node {

	private static final long serialVersionUID = 8636697940678019414L;
	private Svd _exception = null;
	private Blk _blk = null; 
	
	/**
	 * CatchClause
	 *    catch ( SingleVariableDeclaration ) Block
	 */
	public CatClause(String fileName, int startLine, int endLine, ASTNode oriNode) {
		this(fileName, startLine, endLine, oriNode, null);
	}
	
	public CatClause(String fileName, int startLine, int endLine, ASTNode oriNode, Node parent) {
		super(fileName, startLine, endLine, oriNode, parent);
		_nodeType = TYPE.CATCHCLAUSE;
	}

	public CatClause(CatClause another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
	}
	
	public void setException(Svd svd) {
		_exception = svd;
	}
	
	public Svd getException() {
		return _exception;
	}
	
	public void setBody(Blk blk) {
		_blk = blk;
	}
	
	public Blk getBody() {
		return _blk;
	}

	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("catch(");
		stringBuffer.append(_exception.toSrcString());
		stringBuffer.append(")");
		stringBuffer.append(_blk.toSrcString());
		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		_tokens.add("catch");
		_tokens.add("(");
		_tokens.addAll(_exception.tokens());
		_tokens.add(")");
		_tokens.addAll(_blk.tokens());
	}

	@Override
	public Stmt getParentStmt() {
		return getParent().getParentStmt();
	}

	@Override
	public List<Stmt> getChildren() {
		List<Stmt> children = new ArrayList<>(1);
		children.add(_blk);
		return children;
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(2);
		children.add(_exception);
		children.add(_blk);
		return children;
	}
	
	@Override
	public boolean compare(Node other) {
		if(other != null && other instanceof CatClause) {
			CatClause catClause = (CatClause) other;
			return _exception.compare(catClause._exception) && _blk.compare(catClause._blk);
		}
		return false;
	}
}
