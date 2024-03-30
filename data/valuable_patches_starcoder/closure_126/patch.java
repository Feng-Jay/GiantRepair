public class tmp {
  private void tryMinimizeIfBlockExits(  Node trueBlock,  Node falseBlock,  Node ifTree,  int exitType,  String labelName){
    if (matchingExitNode(trueBlock,exitType,labelName)) {
      NodeUtil.removeChild(ifTree.getParent(),trueBlock);
      Node newIfNode=new Node(Token.IF,ifTree.removeFirstChild(),ifTree.removeFirstChild());
      NodeUtil.insertAfter(ifTree,newIfNode,falseBlock);
    }
  }
}
