public class tmp {
  public Number read(  JsonReader in) throws IOException {
    JsonToken jsonToken=in.peek();
switch (jsonToken) {
case NULL:
      in.nextNull();
    return null;
case NUMBER:
  return new LazilyParsedNumber(in.nextString());
default :
throw new JsonSyntaxException("Expecting number, got: " + jsonToken);
}
}
}
