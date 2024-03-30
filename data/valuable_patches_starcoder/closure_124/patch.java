public class tmp {
  private boolean isSafeReplacement(  Node node,  Node replacement){
    if (node.isName()) {
      return true;
    }
    Preconditions.checkArgument(node.isGetProp());
    Node nodeToCheck=node.getFirstChild();
    while (nodeToCheck.isGetProp()) {
      nodeToCheck=nodeToCheck.getFirstChild();
    }
    if (nodeToCheck.isName() && isNameAssignedTo(nodeToCheck.getString(),replacement)) {
      return false;
    }
    return true;
  }
}
