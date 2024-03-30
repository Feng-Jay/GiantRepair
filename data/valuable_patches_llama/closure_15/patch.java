public class tmp {
  public boolean apply(  Node n){
    if (n == null) {
      return false;
    }
    if (n.isCall() && NodeUtil.functionCallHasSideEffects(n)) {
      return true;
    }
    if (n.isNew() && NodeUtil.constructorCallHasSideEffects(n)) {
      return true;
    }
    if (n.isEnter()) {
      return true;
    }
    for (Node c=n.getFirstChild(); c != null; c=c.getNext()) {
      if (apply(c)) {
        return true;
      }
    }
    return false;
  }
}
