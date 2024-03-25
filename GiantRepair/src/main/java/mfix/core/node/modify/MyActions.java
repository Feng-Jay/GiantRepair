package mfix.core.node.modify;

import mfix.core.node.ast.Node;
import mfix.core.node.check.Tracer;

import java.util.List;
import java.util.Objects;

abstract public class MyActions {

    @Override
    public boolean equals(Object o) {
        MyActions oNode = (MyActions) o;
        if(this.getCurNode().equals(oNode.getCurNode())){
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this._curNode);
    }
    protected Node _curNode;
    protected Double _editDistance;

    public String _type;
    protected Tracer _tracer;

    public MyActions(Node curNode){
        _curNode = curNode;
        _editDistance = 0.0;
        _tracer = new Tracer();
        _type = null;
    }

    public String getType(){return _type;}
    public Node getCurNode(){return _curNode;}

    public void setCurNode(Node node) {_curNode = node;}

    public void setTracer(Tracer tracer){_tracer = tracer;}

    public Tracer getTracer(){return _tracer;}

    public List<String> getTrace(){return _tracer.getTrace();}

    public Double getEditDistance(){return _editDistance;}

    public void setEditDistance(Double editDistance){_editDistance = editDistance;}

    public Integer getBaseStartLine(){return _curNode.getStartLine();}

    public Integer getStartPos(){return _curNode.getOriNode().getStartPosition();}

    public Integer getEndPos(){return getStartPos() + _curNode.getOriNode().getLength();}

    public abstract StringBuilder toSrcString();

    @Override
    public String toString() {
        return toSrcString().toString();
    }
}
