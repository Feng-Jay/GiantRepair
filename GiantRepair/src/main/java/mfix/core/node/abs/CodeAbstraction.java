package mfix.core.node.abs;

import mfix.core.node.ast.Node;

public interface CodeAbstraction {

    enum Category{
        API_TOKEN,
        TYPE_TOKEN,
        NAME_TOKEN
    }

    boolean shouldAbstract(String string);

    boolean shouldAbstract(Node node);

    boolean shouldAbstract(String string, Category category);

    boolean shouldAbstract(Node node, Category category);

    CodeAbstraction lazyInit();

}
