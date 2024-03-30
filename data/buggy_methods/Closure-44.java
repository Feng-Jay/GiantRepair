public class tmp {
  void add(  String newcode){
    maybeEndStatement();
    if (newcode.length() == 0) {
      return;
    }
    char c=newcode.charAt(0);
    if ((isWordChar(c) || c == '\\') && isWordChar(getLastChar())) {
      append(" ");
    }
    append(newcode);
  }
}
