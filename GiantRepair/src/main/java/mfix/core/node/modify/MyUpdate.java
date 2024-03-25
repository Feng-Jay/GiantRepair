package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class MyUpdate extends MyActions{
    private Node _formerNode;

    private String _exprString = null;

    Integer _startLine = 0;

    Integer _endLine   = 0;

    public MyUpdate(Node formerNode, Node curNode){
        super(curNode);
        _type = "UPDATE";
        _formerNode = formerNode;
        _startLine  = formerNode.getStartLine();
        _endLine    = formerNode.getEndLine();
    }
    public MyUpdate(MyUpdate another){
        this(another._formerNode, another._curNode);
    }

    public String getExprString(){ return _exprString;}

    public void setExprString(String str){_exprString = str;}

    public Node getFormerNode(){return _formerNode;}

    public Integer getStarLine(){return _startLine;}

    public void setStartLine(Integer line) {_startLine = line;}

    public Integer getEndLine(){return _endLine;}

    public void setEndLine(Integer line){_endLine = line;}
    @Override
    public StringBuilder toSrcString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE\n");
        builder.append("FROM:\n");
        builder.append(_formerNode.getStartLine()+":"+_formerNode.toString()+"\n");
        builder.append("TO:\n");
        builder.append(_curNode.getStartLine()+":"+_curNode.toString()+"\n");
        return builder;
    }
}
