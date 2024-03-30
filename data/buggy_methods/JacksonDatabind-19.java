public class tmp {
  private JavaType _mapType(  Class<?> rawClass){
    JavaType[] typeParams=findTypeParameters(rawClass,Map.class);
    if (typeParams == null) {
      return MapType.construct(rawClass,_unknownType(),_unknownType());
    }
    if (typeParams.length != 2) {
      throw new IllegalArgumentException("Strange Map type " + rawClass.getName() + ": can not determine type parameters");
    }
    return MapType.construct(rawClass,typeParams[0],typeParams[1]);
  }
}
