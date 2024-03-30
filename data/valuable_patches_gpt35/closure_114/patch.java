public class tmp {
  private void recordAssignment(  NodeTraversal t,  Node n,  Node recordNode){
    Node nameNode=n.getFirstChild();
    Node parent=n.getParent();
    NameInformation ns=createNameInformation(t,nameNode);
    if (ns != null) {
      if (parent.isFor() && !NodeUtil.isForIn(parent)) {
        if (parent.getFirstChild().getNext() != n) {
          recordDepScope(recordNode,ns);
        }
 else {
          recordDepScope(nameNode,ns);
        }
      }
 else       if (parent.isCall() && parent.getFirstChild() == n) {
        recordDepScope(recordNode,ns);
      }
 else {
        recordDepScope(nameNode,ns);
      }
    }
  }
}
