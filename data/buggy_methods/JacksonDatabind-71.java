public class tmp {
  public static StdKeyDeserializer forType(  Class<?> raw){
    int kind;
    if (raw == String.class || raw == Object.class) {
      return StringKD.forType(raw);
    }
 else     if (raw == UUID.class) {
      kind=TYPE_UUID;
    }
 else     if (raw == Integer.class) {
      kind=TYPE_INT;
    }
 else     if (raw == Long.class) {
      kind=TYPE_LONG;
    }
 else     if (raw == Date.class) {
      kind=TYPE_DATE;
    }
 else     if (raw == Calendar.class) {
      kind=TYPE_CALENDAR;
    }
 else     if (raw == Boolean.class) {
      kind=TYPE_BOOLEAN;
    }
 else     if (raw == Byte.class) {
      kind=TYPE_BYTE;
    }
 else     if (raw == Character.class) {
      kind=TYPE_CHAR;
    }
 else     if (raw == Short.class) {
      kind=TYPE_SHORT;
    }
 else     if (raw == Float.class) {
      kind=TYPE_FLOAT;
    }
 else     if (raw == Double.class) {
      kind=TYPE_DOUBLE;
    }
 else     if (raw == URI.class) {
      kind=TYPE_URI;
    }
 else     if (raw == URL.class) {
      kind=TYPE_URL;
    }
 else     if (raw == Class.class) {
      kind=TYPE_CLASS;
    }
 else     if (raw == Locale.class) {
      FromStringDeserializer<?> deser=FromStringDeserializer.findDeserializer(Locale.class);
      return new StdKeyDeserializer(TYPE_LOCALE,raw,deser);
    }
 else     if (raw == Currency.class) {
      FromStringDeserializer<?> deser=FromStringDeserializer.findDeserializer(Currency.class);
      return new StdKeyDeserializer(TYPE_CURRENCY,raw,deser);
    }
 else {
      return null;
    }
    return new StdKeyDeserializer(kind,raw);
  }
}
