public class tmp {
  protected void XmlSerializerProvider(  XmlSerializerProvider src){
    super(src);
    _rootNameLookup=new RootNameLookup(src._rootNameLookup);
  }
}
