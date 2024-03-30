public class tmp {
  private void traverse(  Node node){
    if (!shouldVisit(node)) {
      return;
    }
    int visits=0;
    do {
      Node c=node.getFirstChild();
      while (c != null) {
        traverse(c);
        Node next=c.getNext();
        c=next;
      }
      visit(node);
      visits++;
      Preconditions.checkState(visits < 10000,"too many interations");
    }
 while (shouldRetraverse(node));
    exitNode(node);
  }
}
