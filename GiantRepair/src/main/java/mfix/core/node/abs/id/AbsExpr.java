package mfix.core.node.abs.id;


public class AbsExpr extends AbsNode {

    private static final long serialVersionUID = 2291718963004089208L;

    private final static String IDENTIFIER="EXPR";

    public AbsExpr(int id) {
        super(id, IDENTIFIER + '_' + id);
    }

    @Override
    public String toString() {
        return IDENTIFIER;
    }
}
