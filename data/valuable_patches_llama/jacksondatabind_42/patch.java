public class tmp {
  protected Object _deserializeFromEmptyString() throws IOException {
    if (_kind == STD_URI) {
      return URI.create("");
    }
    if (_kind == STD_LOCALE) {
      return Locale.ENGLISH;
    }
    return super._deserializeFromEmptyString();
  }
}
