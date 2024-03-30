public class tmp {
  private static boolean isReduceableFunctionExpression(  Node n){
    return NodeUtil.isFunctionExpression(n) && !NodeUtil.hasSideEffects(n.getParent());
  }
}
