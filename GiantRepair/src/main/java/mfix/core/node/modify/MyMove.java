package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class MyMove extends MyActions{

    private Node _formerNode;

    public MyMove(Node formerNode, Node curNode){
        super(curNode);
        _formerNode = formerNode;
        _type = "MOVE";
    }

    public Node getFormerNode(){return _formerNode;}

    public Integer getFromerStartLine(){return _formerNode.getStartLine();}

    public Integer getFromerEndLine(){return _formerNode.getEndLine();}

    public Integer getCurStartLine(){return _curNode.getStartLine();}

    public Integer getCurEndLine(){return _curNode.getEndLine();}

    @Override
    public StringBuilder toSrcString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Move\n");
        builder.append("FROM:\n");

        builder.append("Former Line ")
                .append(getFromerStartLine())
                .append(":")
                .append(_formerNode.toString())
                .append("\n");

        builder.append("TO:\n");

        builder.append("Current Line ")
                .append(_curNode.getStmtParent().getStartLine())
                .append(":")
                .append(_curNode.getStmtParent().getNodeType())
                .append("\n");
        return builder;
    }

}
