public class tmp {
  public boolean equals(  Object o){
    if (this == o)     return true;
    if (o == null || getClass() != o.getClass())     return false;
    if (!super.equals(o))     return false;
    Element element=(Element)o;
    if (!Objects.equals(name,element.name))     return false;
    return value.equals(element.value);
  }
}
