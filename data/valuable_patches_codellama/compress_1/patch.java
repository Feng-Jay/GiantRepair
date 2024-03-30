public class tmp {
  public void close() throws IOException {
    if (!this.closed) {
      if (this.name != null) {
        this.delete();
      }
      super.close();
      this.closed=true;
    }
  }
}
