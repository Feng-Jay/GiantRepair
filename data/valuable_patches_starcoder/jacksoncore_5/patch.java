public class tmp {
  private static int _parseIndex(  String str){
    final int len=str.length();
    if (len > 10) {
      return -1;
    }
    for (int i=0; i < len; ++i) {
      char c=str.charAt(i);
      if (c > '9' || c < '0') {
        return -1;
      }
    }
    return NumberInput.parseInt(str);
  }
}
