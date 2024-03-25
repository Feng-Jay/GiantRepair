
package mfix.core.node.ast.expr;

import org.eclipse.jdt.core.dom.ASTNode;


public abstract class Label extends Expr {

	private static final long serialVersionUID = -6660200671704024539L;

	/**
	 * Name:
     *	SimpleName
     *	QualifiedName
	 */
	public Label(String fileName, int startLine, int endLine, ASTNode node) {
		super(fileName, startLine, endLine, node);
	}

}
