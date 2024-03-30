public class tmp {
  public int getLength(){
    if (getBaseValue() == null) {
      return 0;
    }
    return ValueUtils.getLength(getBaseValue());
  }
}
