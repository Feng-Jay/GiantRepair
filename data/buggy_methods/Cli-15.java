public class tmp {
  public List getValues(  final Option option,  List defaultValues){
    List valueList=(List)values.get(option);
    if ((valueList == null) || valueList.isEmpty()) {
      valueList=defaultValues;
    }
    if ((valueList == null) || valueList.isEmpty()) {
      valueList=(List)this.defaultValues.get(option);
    }
    return valueList == null ? Collections.EMPTY_LIST : valueList;
  }
}
