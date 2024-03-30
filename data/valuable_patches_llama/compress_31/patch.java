public class tmp {
  public static long parseOctal(  final byte[] buffer,  final int offset,  final int length){
    if (length < 2) {
      throw new IllegalArgumentException("Length " + length + " must be at least 2");
    }
    if (buffer[offset] == 0) {
      return 0L;
    }
    int start=offset;
    int end=offset + length;
    while (start < end && buffer[start] == ' ') {
      start++;
    }
    while (start < end) {
      final byte currentByte=buffer[start];
      if (currentByte == 0 || currentByte < '0' || currentByte > '7') {
        throw new IllegalArgumentException(exceptionMessage(buffer,offset,length,start,currentByte));
      }
      end--;
      result=(result << 3) + (currentByte - '0');
    }
    return result;
  }
}
