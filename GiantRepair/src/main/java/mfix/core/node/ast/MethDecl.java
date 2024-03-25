
package mfix.core.node.ast;

import mfix.core.node.ast.expr.Expr;
import mfix.core.node.ast.expr.SName;
import mfix.core.node.ast.stmt.Blk;
import mfix.core.node.ast.stmt.Stmt;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MethDecl extends Node {

    private static final long serialVersionUID = -4279492715496549645L;
    private List<String> _modifiers = new ArrayList<>(5);
    private String _retTypeStr;
    private transient Type _retType;
    private SName _name;
    private List<Expr> _arguments;
    private Blk _body;
    private List<String> _throws;

    public MethDecl(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        _nodeType = TYPE.METHDECL;
        _retType = AST.newAST(AST.JLS8).newWildcardType();
        _retTypeStr = _retType.toString();
    }

    public void setModifiers(List<String> modifiers) {
        _modifiers = modifiers;
    }

    public List<String> getModifiers() {
        return _modifiers;
    }

    public void setThrows(List<String> throwTypes) {
        _throws = throwTypes;
    }

    public List<String> getThrows() {
        return _throws;
    }

    public void setRetType(Type type) {
        if (type != null) {
            _retType = type;
            _retTypeStr = type.toString();
        }
    }

    public Type getRetType() {
        return _retType;
    }

    public String getRetTypeStr() {
        return _retTypeStr;
    }

    public void setName(SName name) {
        _name = name;
    }

    public SName getName() {
        return _name;
    }

    public void setArguments(List<Expr> arguments) {
        _arguments = arguments;
    }

    public List<Expr> getArguments() {
        return _arguments;
    }

    public void setBody(Blk body) {
        _body = body;
    }

    public Blk getBody() {
        return _body;
    }

    @Override
    public StringBuffer toSrcString() {
        StringBuffer stringBuffer = new StringBuffer();
        for (Object modifier : _modifiers) {
            stringBuffer.append(modifier.toString() + " ");
        }
        if (!_retTypeStr.equals("?")) {
            stringBuffer.append(_retTypeStr + " ");
        }
        stringBuffer.append(_name.toSrcString());
        stringBuffer.append("(");
        if (_arguments != null && _arguments.size() > 0) {
            stringBuffer.append(_arguments.get(0).toSrcString());
            for (int i = 1; i < _arguments.size(); i++) {
                stringBuffer.append("," + _arguments.get(i).toSrcString());
            }
        }
        stringBuffer.append(")");
        if (_throws != null && _throws.size() > 0) {
            stringBuffer.append(" throws " + _throws.get(0));
            for (int i = 1; i < _throws.size(); i++) {
                stringBuffer.append("," + _throws.get(i));
            }
        }
        if (_body == null) {
            stringBuffer.append(";");
        } else {
            stringBuffer.append(_body.toSrcString());
        }
        return stringBuffer;
    }


    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();

        for (Object modifier : _modifiers) {
            _tokens.add(modifier.toString());
        }
        if (!_retType.toString().equals("?")) {
            _tokens.add(_retType.toString());
        }
        _tokens.addAll(_name.tokens());
        _tokens.add("(");
        if (_arguments != null && _arguments.size() > 0) {
            _tokens.addAll(_arguments.get(0).tokens());
            for (int i = 1; i < _arguments.size(); i++) {
                _tokens.addAll(_arguments.get(i).tokens());
            }
        }
        _tokens.add(")");
        if (_body == null) {
            _tokens.add(";");
        } else {
            _tokens.addAll(_body.tokens());
        }
    }

    @Override
    public Stmt getParentStmt() {
        return null;
    }

    @Override
    public List<Stmt> getChildren() {
        List<Stmt> children = new ArrayList<>(1);
        if (_body != null) {
            children.add(_body);
        }
        return children;
    }

    @Override
    public List<Node> getAllChildren() {
        List<Node> children = new ArrayList<>(_arguments);
        if (_body == null) {
            return children;
        } else {
            children.add(_body);
            return children;
        }
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof MethDecl) {
            MethDecl methDecl = (MethDecl) other;
            match = _name.compare(methDecl._name) && (_arguments.size() == methDecl._arguments.size());
            for (int i = 0; match && i < _arguments.size(); i++) {
                match = match && _arguments.get(i).compare(methDecl._arguments.get(i));
            }
            if (_body != null && methDecl.getBody() != null) {
                _body.compare(methDecl.getBody());
            }
        }
        return match;
    }
}