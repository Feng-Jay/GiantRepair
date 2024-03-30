public class tmp {
  public int indexOf(  String str,  int startIndex){
    if (str == null) {
      return -1;
    }
    int strLen=str.length();
    if (strLen == 1) {
      return indexOf(str.charAt(0),startIndex);
    }
    if (strLen == 0) {
      return startIndex < size ? startIndex : size;
    }
    if (startIndex < 0) {
      startIndex=0;
    }
    int endIndex=size - strLen;
    if (startIndex > endIndex) {
      return -1;
    }
    char[] thisBuf=buffer;
    outer:     for (int i=startIndex; i <= endIndex; i++) {
      for (int j=0; j < strLen; j++) {
        if (str.charAt(j) != thisBuf[i + j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }
}
