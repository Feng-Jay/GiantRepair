public class tmp {
  public boolean apply(  Node n){
    if (n == null) {
      return false;
    }
    if (n.isCall() || n.isNew()) {
      if (!NodeUtil.isExpressionResultUsed(n)) {
        return false;
      }
      if (NodeUtil.isSideEffectFree(n)) {
        return false;
      }
      return true;
    }
    if (n.isFunction()) {
      return false;
    }
    for (Node c=n.getFirstChild(); c != null; c=c.getNext()) {
      if (c.isFunction()) {
        return false;
      }
      if (!ControlFlowGraph.isEnteringNewCfgNode(c) && apply(c)) {
        return true;
      }
    }
    return false;
  }
}
