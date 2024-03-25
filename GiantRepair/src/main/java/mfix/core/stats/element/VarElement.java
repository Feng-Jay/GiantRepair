package mfix.core.stats.element;

public class VarElement extends Element {
    protected String _varType = null;

    public VarElement(String name, String fileName) {
        super(name, fileName);
    }

    public VarElement(String name, String type, String fileName) {
        super(name, fileName);
        _varType = type;
    }

    public void setVarType(String varType) {
        _varType = varType;
    }
}
