package mfix.core.node.modify;

import mfix.core.node.ast.Node;

import java.util.List;

public class Wrap extends MyActions{
    private List<Node> _stmts;

    public Wrap(Node curNode, List<Node> stmts){
        super(curNode);
        _type = "WRAP";
        _stmts   = stmts;
    }

    public Wrap(Wrap another){
        this(another._curNode, another._stmts);
    }

    public List<Node> getStmts() {return  _stmts;}

    @Override
    public StringBuilder toSrcString(){
        StringBuilder builder = new StringBuilder();
        builder.append("Wrap:\n");
        builder.append(_curNode.getNodeType().toString()+"\n");
        builder.append("On:\n");
        builder.append(_stmts);
        return builder;
    }
}
