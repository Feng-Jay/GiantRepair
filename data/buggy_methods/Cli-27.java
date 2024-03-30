public class tmp {
  public void setSelected(  Option option) throws AlreadySelectedException {
    if (option == null) {
      selected=null;
      return;
    }
    if (selected == null || selected.equals(option.getOpt())) {
      selected=option.getOpt();
    }
 else {
      throw new AlreadySelectedException(this,option);
    }
  }
}
