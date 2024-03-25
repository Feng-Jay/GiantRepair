package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.AnonymousClassDecl;
import org.eclipse.jdt.core.dom.ASTNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClassInstCreation extends Expr {

    private static final long serialVersionUID = -2405461094348344933L;
    private Expr _expression = null;
    private MType _classType = null;
    private ExprList _arguments = null;
    private AnonymousClassDecl _decl = null;

    /**
     * ClassInstanceCreation: [ Expression . ] new [ < Type { , Type } > ] Type
     * ( [ Expression { , Expression } ] ) [ AnonymousClassDeclaration ]
     */
    public ClassInstCreation(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CLASSCREATION;
    }
    public ClassInstCreation(ClassInstCreation another){
        this(another._fileName, another._startLine, another._endLine, another._oriNode);
        this._expression = another._expression;
        this._decl = another._decl;
        // check type, parameters, declarations...
    }

    public void setExpression(Expr expression) {
        _expression = expression;
    }

    public void setClassType(MType classType) {
        _classType = classType;
    }

    public void setArguments(ExprList arguments) {
        _arguments = arguments;
    }

    public void setAnonymousClassDecl(AnonymousClassDecl decl) {
        _decl = decl;
    }

    public MType getClassType() {
        return _classType;
    }

    public Expr getExpression() {
        return _expression;
    }

    public ExprList getArguments() {
        return _arguments;
    }

    public AnonymousClassDecl getDecl() {
        return _decl;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (_expression != null) {
            stringBuffer.append(_expression.toSrcString());
            stringBuffer.append(".");
        }
        stringBuffer.append("new ");
        stringBuffer.append(_classType.toSrcString());
        stringBuffer.append("(");
        stringBuffer.append(_arguments.toSrcString());
        stringBuffer.append(")");
        if (_decl != null) {
            stringBuffer.append(_decl.toSrcString());
        }
        return stringBuffer;
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
        if (_expression != null) {
            _tokens.addAll(_expression.tokens());
            _tokens.add(".");
        }
        _tokens.add("new");
        _tokens.addAll(_classType.tokens());
        _tokens.add("(");
        _tokens.addAll(_arguments.tokens());
        _tokens.add(")");
        if (_decl != null) {
            _tokens.addAll(_decl.tokens());
        }
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof ClassInstCreation) {
            ClassInstCreation classInstCreation = (ClassInstCreation) other;
            match = _expression == null ? (classInstCreation._expression == null) :
					_expression.compare(classInstCreation._expression);
            match =
					match && _classType.compare(classInstCreation._classType) && _arguments.compare(classInstCreation._arguments);
            if (_decl == null) {
                match = match && (classInstCreation._decl == null);
            } else {
                match = match && _decl.compare(classInstCreation._decl);
            }
        }
        return match;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(4);
        if (_expression != null) {
            children.add(_expression);
        }
        children.add(_classType);
        children.add(_arguments);
        if (_decl != null) {
            children.add(_decl);
        }
        return children;
    }

}
