public class tmp {
  private boolean isPrototypePropertyAssign(  Node assign){
    Node n=assign.getFirstChild();
    if (n != null && NodeUtil.isVarOrSimpleAssignLhs(n,assign) && n.getType() == Token.GETPROP) {
      boolean isChainedProperty=n.getFirstChild().getType() == Token.GETPROP;
      if (isChainedProperty) {
        Node child=n.getFirstChild().getFirstChild().getNext();
        if (child.getType() == Token.STRING && child.getString().equals("prototype")) {
          return true;
        }
      }
    }
    return false;
  }
}
