public class tmp {
  private static final int DEFAULT_SEGMENT_SIZE=100;
  private static final int MAX_SEGMENT_SIZE=1000;
  public char[] expandCurrentSegment(){
    final char[] curr=_currentSegment;
    final int len=curr.length;
    int newLen=len < DEFAULT_SEGMENT_SIZE ? DEFAULT_SEGMENT_SIZE : len + (len >> 1);
    if (newLen > MAX_SEGMENT_SIZE) {
      newLen=len + (len >> 2);
    }
    return (_currentSegment=Arrays.copyOf(curr,newLen));
  }
}
