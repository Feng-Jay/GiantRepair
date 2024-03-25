

package mfix.core.node.ast;

import mfix.common.util.Utils;

import java.io.Serializable;

public class Variable implements Serializable {

    private static final long serialVersionUID = 6612954937479653066L;
    private String _name;
    private String _type;

    public Variable(String name, String type) {
        _name = name;
        _type = type;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Variable)) {
            return false;
        }
        Variable var = (Variable) obj;
        // currently consider name only
        // if type is needed, update here
        return Utils.safeStringEqual(_name, var._name);
//                && Utils.safeStringEqual(_type, var._type);
    }
}
