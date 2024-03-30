public class tmp {
  public void writeEmbeddedObject(  Object object) throws IOException {
    if (object == null) {
      writeNull();
      return;
    }
    if (object instanceof JsonNode) {
      writeTree((JsonNode)object);
      return;
    }
    if (object instanceof byte[]) {
      writeBinary((byte[])object);
      return;
    }
    throw new JsonGenerationException("No native support for writing embedded objects of type " + object.getClass().getName(),this);
  }
}
