public class tmp {
  private static ZipLong unixTimeToZipLong(  long l){
    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("X5455 timestamps must fit in a signed 32 bit integer: " + l);
    }
    return new ZipLong((int)l);
  }
}
