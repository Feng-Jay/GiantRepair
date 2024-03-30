public class tmp {
  InputStream decode(  final InputStream in,  final Coder coder,  byte[] password) throws IOException {
    int propsByte=coder.properties[0] & 0xFF;
    long dictSize=coder.properties[1] & 0xFF;
    for (int i=1; i < 4; i++) {
      dictSize|=((long)coder.properties[i + 1] & 0xFF) << (8 * i);
    }
    if (dictSize > LZMAInputStream.DICT_SIZE_MAX) {
      throw new IOException("Dictionary larger than 4GiB maximum size");
    }
    return new LZMAInputStream(in,-1,propsByte,(int)dictSize);
  }
}
