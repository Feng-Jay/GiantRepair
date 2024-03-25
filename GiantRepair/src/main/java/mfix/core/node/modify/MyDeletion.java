package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class MyDeletion extends MyActions{


    public MyDeletion(Node curNode){
        super(curNode);
        _type = "DELETE";
    }

    public Integer getStartLine(){return _curNode.getStartLine();}

    public Integer getEndLine(){return _curNode.getEndLine();}

    @Override
    public StringBuilder toSrcString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DELETE:\n");
        builder.append("Original line: "+_curNode.getStartLine()+_curNode.toString()+"\n");
        return builder;
    }
}
