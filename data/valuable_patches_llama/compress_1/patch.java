public class tmp {
  public void close() throws IOException {
    if (!this.closed) {
      if (this.markSupported) {
        this.mark();
      }
      super.close();
      this.closed=true;
    }
  }
}
