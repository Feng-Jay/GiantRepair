public class tmp {
  public void ZipArchiveInputStream(  InputStream inputStream,  String encoding,  boolean useUnicodeExtraFields,  boolean allowStoredEntriesWithDataDescriptor){
    zipEncoding=ZipEncodingHelper.getZipEncoding(encoding);
    this.useUnicodeExtraFields=useUnicodeExtraFields;
    in=new PushbackInputStream(inputStream,buf.capacity());
    this.allowStoredEntriesWithDataDescriptor=allowStoredEntriesWithDataDescriptor;
    buf.clear();
  }
}
