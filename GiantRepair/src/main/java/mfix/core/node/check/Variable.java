package mfix.core.node.check;

import mfix.common.util.Utils;
import mfix.core.node.ast.Node;

import java.util.Objects;

public class Variable extends Members{
    private String _name;
    private final String _type;

    private final Node _node;

    public Variable(Integer line, String name, String type, Node node){
        super(line);
        _name = name;
        _type = type;
        _node = node;
    }
    public Variable(Variable another, String type){
        super(another.getLine());
        _name = another._name;
        _node = another._node;
        _type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable v = (Variable) o;
        return this.getLine().equals(v.getLine()) && Utils.safeStringEqual(this._name, v._name) && Utils.safeStringEqual(this._type, v._type);
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.getLine(), this._type, this._name);
    }


    public String getType(){
        return _type;
    }

    public String getName(){
        return _name;
    }

    public Node getNode(){return _node;}

    public void setName(String newName){
        _name = newName;
    }

    public String toString(){
        return _type + " " + _name;
    }

}
