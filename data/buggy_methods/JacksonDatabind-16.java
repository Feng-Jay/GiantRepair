public class tmp {
  protected final boolean _add(  Annotation ann){
    if (_annotations == null) {
      _annotations=new HashMap<Class<? extends Annotation>,Annotation>();
    }
    Annotation previous=_annotations.put(ann.annotationType(),ann);
    return (previous != null) && previous.equals(ann);
  }
}
