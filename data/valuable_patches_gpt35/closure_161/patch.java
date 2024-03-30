public class tmp {
  private Node tryFoldArrayAccess(  Node n,  Node left,  Node right){
    Node parent=n.getParent();
    if (n.isAssign()) {
      return n;
    }
    if (!right.isNumber()) {
      return n;
    }
    double index=right.getDouble();
    int intIndex=(int)index;
    if (intIndex != index) {
      error(INVALID_GETELEM_INDEX_ERROR,right);
      return n;
    }
    if (intIndex < 0) {
      error(INDEX_OUT_OF_BOUNDS_ERROR,right);
      return n;
    }
    Node elem=left.getFirstChild();
    for (int i=0; elem != null && i < intIndex; i++) {
      elem=elem.getNext();
    }
    if (elem == null) {
      error(INDEX_OUT_OF_BOUNDS_ERROR,right);
      return n;
    }
    if (elem.isEmpty()) {
      elem=NodeUtil.newUndefinedNode(elem);
    }
 else {
      left.removeChild(elem);
    }
    parent.replaceChild(n,elem);
    reportCodeChange();
    return elem;
  }
}
