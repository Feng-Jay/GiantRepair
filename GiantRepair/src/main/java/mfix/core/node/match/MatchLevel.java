

package mfix.core.node.match;

public enum MatchLevel {
    FUZZY, // both type and name are ignored
    TYPE,  // type info should be matched if not abstracted
    NAME,  // name info should be matched if not abstracted
    ALL,   // both type and name should be matched if not abstract
    AST    // node type in the ast should be matched (i.e., StringLiteral can only match StringLiteral)
}
