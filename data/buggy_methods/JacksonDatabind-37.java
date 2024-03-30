public class tmp {
  protected JavaType _narrow(  Class<?> subclass){
    if (_class == subclass) {
      return this;
    }
    return new SimpleType(subclass,_bindings,_superClass,_superInterfaces,_valueHandler,_typeHandler,_asStatic);
  }
}
