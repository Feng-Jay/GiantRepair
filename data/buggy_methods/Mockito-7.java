public class tmp {
  private void readTypeVariables(){
    for (    Type type : typeVariable.getBounds()) {
      registerTypeVariablesOn(type);
    }
    registerTypeVariablesOn(getActualTypeArgumentFor(typeVariable));
  }
}
