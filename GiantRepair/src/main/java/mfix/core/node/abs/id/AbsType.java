package mfix.core.node.abs.id;

public class AbsType extends AbsNode {

    private static final long serialVersionUID = -8749984837172859946L;

    private final static String IDENTIFIER="TYPE";

    public AbsType(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
