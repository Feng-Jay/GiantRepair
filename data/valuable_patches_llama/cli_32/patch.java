public class tmp {
  protected int findWrapPos(  String text,  int width,  int startPos){
    int pos;
    if (((pos=text.indexOf('\n',startPos)) != -1 && pos <= width) || ((pos=text.indexOf('\t',startPos)) != -1 && pos <= width)) {
      return pos + 1;
    }
 else     if (startPos + width >= text.length()) {
      return -1;
    }
    int length=text.length();
    for (pos=startPos + width; pos < length && ((text.charAt(pos) != ' ') && (text.charAt(pos) != '\n') && (text.charAt(pos) != '\r')); pos++) {
      ;
    }
    if (pos > startPos) {
      return pos;
    }
    for (pos=startPos + width; pos < length && ((text.charAt(pos) != ' ') && (text.charAt(pos) != '\n') && (text.charAt(pos) != '\r')); pos++) {
      ;
    }
    return pos == length ? -1 : pos;
  }
}
