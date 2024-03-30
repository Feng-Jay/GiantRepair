public class tmp {
  protected void XmlSerializerProvider(  XmlSerializerProvider src){
    super(src);
    if (src._rootNameLookup != null) {
      _rootNameLookup=new XmlRootNameLookup();
      _rootNameLookup.addAll(src._rootNameLookup);
    }
  }
}
