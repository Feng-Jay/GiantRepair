package mfix.core.node.abs.id;

import java.io.Serializable;

public abstract class AbsNode implements Serializable {

    private static final long serialVersionUID = 8148656586590047176L;

    private int _id;
    private String _name;

    protected AbsNode(int id, String name) {
        _id = id;
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public int getId() {
        return _id;
    }

    @Override
    public String toString() {
        return _name;
    }
}
