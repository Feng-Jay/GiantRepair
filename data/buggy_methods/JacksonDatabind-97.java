public class tmp {
  public final void serialize(  JsonGenerator gen,  SerializerProvider ctxt) throws IOException {
    if (_value == null) {
      ctxt.defaultSerializeNull(gen);
    }
 else     if (_value instanceof JsonSerializable) {
      ((JsonSerializable)_value).serialize(gen,ctxt);
    }
 else {
      gen.writeObject(_value);
    }
  }
}
