public class tmp {
  private void popStackToClose(  String endTag){
    String elName=endTag.toLowerCase(Locale.ROOT);
    Element firstFound=null;
    for (int pos=stack.size() - 1; pos >= 0; pos--) {
      Element next=stack.get(pos);
      if (next.nodeName().equals(elName)) {
        firstFound=next;
        break;
      }
    }
    if (firstFound == null)     return;
    for (int pos=stack.size() - 1; pos >= 0; pos--) {
      Element next=stack.get(pos);
      stack.remove(pos);
      if (next == firstFound)       break;
    }
  }
}
