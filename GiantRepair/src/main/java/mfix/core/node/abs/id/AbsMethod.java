package mfix.core.node.abs.id;

public class AbsMethod extends AbsNode {

    private static final long serialVersionUID = 2782218564013313007L;
    private final static String IDENTIFIER="METH";

    public AbsMethod(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
