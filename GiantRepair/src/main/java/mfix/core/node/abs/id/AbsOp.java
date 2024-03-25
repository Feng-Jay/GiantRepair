package mfix.core.node.abs.id;

public class AbsOp extends AbsNode {

    private static final long serialVersionUID = 4124458532960457523L;

    private final static String IDENTIFIER="<OP>";

    public AbsOp(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
