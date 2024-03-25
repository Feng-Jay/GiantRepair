package mfix.core.node.ast.expr;

import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CharacterLiteral;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class CharLiteral extends Expr {

    private static final long serialVersionUID = 995719993109521913L;
    private char _value = ' ';
    private String _valStr = null;

    /**
     * Character literal nodes.
     */
    public CharLiteral(String fileName, int startLine, int endLine, ASTNode node) {
        super(fileName, startLine, endLine, node);
        _nodeType = TYPE.CLITERAL;
    }

    public void setValue(CharacterLiteral literal) {
        _value = literal.charValue();
        _valStr = literal.getEscapedValue();
    }

    @Deprecated
    public void setValue(char value) {
        _value = value;
        _valStr = "" + _value;
        _valStr = _valStr.replace("\\", "\\\\").replace("\'", "\\'").replace("\"", "\\\"").replace("\n", "\\n")
                .replace("\b", "\\b").replace("\t", "\\t").replace("\r", "\\r").replace("\f", "\\f")
                .replace("\0", "\\0");
    }

    public String getStringValue() {
        return toSrcString().toString();
    }

    public char getValue() {
        return _value;
    }

    @Override
    public StringBuffer toSrcString() {
//        return new StringBuffer("\'" + _valStr + "\'");
        return new StringBuffer(_valStr);
    }

    @Override
    protected void tokenize() {
        _tokens = new LinkedList<>();
//        _tokens.add("\'" + _valStr + "\'");
        _tokens.add(_valStr);
    }

    @Override
    public List<Node> getAllChildren() {
        return new ArrayList<>(0);
    }

    @Override
    public boolean compare(Node other) {
        boolean match = false;
        if (other != null && other instanceof CharLiteral) {
            CharLiteral charLiteral = (CharLiteral) other;
            match = (_value == charLiteral._value);
        }
        return match;
    }
}
