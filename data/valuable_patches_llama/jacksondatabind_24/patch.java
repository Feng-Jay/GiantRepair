public class tmp {
  public BaseSettings withDateFormat(  DateFormat df){
    if (df == null) {
      if (_dateFormat != null) {
        df=_dateFormat;
      }
    }
    if (df == _dateFormat) {
      return this;
    }
    return new BaseSettings(_classIntrospector,_annotationIntrospector,_visibilityChecker,_propertyNamingStrategy,_typeFactory,_typeResolverBuilder,df,_handlerInstantiator,_locale,_timeZone,_defaultBase64);
  }
}
