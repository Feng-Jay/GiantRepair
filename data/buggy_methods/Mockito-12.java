public class tmp {
  public Class getGenericType(  Field field){
    Type generic=field.getGenericType();
    if (generic != null && generic instanceof ParameterizedType) {
      Type actual=((ParameterizedType)generic).getActualTypeArguments()[0];
      return (Class)actual;
    }
    return Object.class;
  }
}
