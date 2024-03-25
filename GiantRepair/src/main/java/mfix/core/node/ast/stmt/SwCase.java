
package mfix.core.node.ast.stmt;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.match.MatchLevel;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.expr.Expr;
import mfix.core.node.match.metric.FVector;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SwCase extends Stmt {

	private static final long serialVersionUID = 3371970934436172117L;
	private Expr _expression = null;
	
	/**
	 * SwitchCase:
     *           case Expression  :
     *           default :
	 */
	public SwCase(String fileName, int startLine, int endLine, ASTNode node) {
		this(fileName, startLine, endLine, node, null);
	}
	
	public SwCase(String fileName, int startLine, int endLine, ASTNode node, Node parent) {
		super(fileName, startLine, endLine, node, parent);
		_nodeType = TYPE.SWCASE;
	}

	public SwCase(SwCase another){
		this(another._fileName, another._startLine, another._endLine, another._oriNode, null);
	}
	
	public void setExpression(Expr expression){
		_expression = expression;
	}
	
	public Expr getExpression(){
		return _expression;
	}
	
	
	@Override
	public StringBuffer toSrcString() {
		StringBuffer stringBuffer = new StringBuffer();
		if (_expression == null) {
			stringBuffer.append("default :");
		} else {
			stringBuffer.append("case ");
			stringBuffer.append(_expression.toSrcString());
			stringBuffer.append(" :");
		}

		return stringBuffer;
	}

	@Override
	protected void tokenize() {
		_tokens = new LinkedList<>();
		if(_expression == null) {
			_tokens.add("default");
		} else {
			_tokens.add("case");
			_tokens.addAll(_expression.tokens());
		}
		_tokens.add(":");
	}
	
	@Override
	public List<Node> getAllChildren() {
		List<Node> children = new ArrayList<>(1);
		if(_expression != null) {
			children.add(_expression);
		}
		return children;
	}
	
	@Override
	public List<Stmt> getChildren() {
		return new ArrayList<>(0);
	}
	
	@Override
	public boolean compare(Node other) {
		if(other != null && other instanceof SwCase) {
			SwCase swCase = (SwCase) other;
			if(_expression != null) {
				return _expression.compare(swCase._expression);
			} else {
				return swCase._expression == null;
			}
		}
		return false;
	}

}
