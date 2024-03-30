public class tmp {
  public int writeValue(){
    if (_type == TYPE_OBJECT) {
      if (_gotName) {
        return STATUS_OK_AS_IS;
      }
      _gotName=true;
      ++_index;
      return STATUS_OK_AFTER_COLON;
    }
    if (_type == TYPE_ARRAY) {
      int ix=_index;
      ++_index;
      return (ix < 0) ? STATUS_OK_AS_IS : STATUS_OK_AFTER_COMMA;
    }
    if (_index != 0) {
      ++_index;
    }
    return STATUS_OK_AS_IS;
  }
}
