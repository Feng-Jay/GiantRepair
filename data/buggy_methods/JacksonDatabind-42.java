public class tmp {
  protected Object _deserializeFromEmptyString() throws IOException {
    if (_kind == STD_URI) {
      return URI.create("");
    }
    return super._deserializeFromEmptyString();
  }
}
