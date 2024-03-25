package mfix.core.node.modify;

import mfix.core.node.ast.Node;

public class MyInsertion extends MyActions{
    private Integer _pos;
    private Integer _line;

    public MyInsertion(Node curNode, int pos, int lineNum){
        super(curNode);
        _type = "INSERT";
        _pos     = pos;
        _line    = lineNum;
    }
    public MyInsertion(MyInsertion another){
        this(another._curNode, another._pos, another._line);
    }

    public Integer getPos(){return  _pos;}

    public void setPos(Integer pos){ _pos = pos;}

    public Integer getLine(){return _line;}
    public void setLine(Integer line){ _line = line;}

    @Override
    public StringBuilder toSrcString() {
       StringBuilder builder = new StringBuilder();
       builder.append("INSERT:\n");
       builder.append(_curNode.getStartLine()+":"+_curNode.toString());
       builder.append("\nTO:\n");
       builder.append(_line+"\n");
       return builder;
    }
}
