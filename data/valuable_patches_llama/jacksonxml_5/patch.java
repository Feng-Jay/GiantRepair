public class tmp {
  protected void XmlSerializerProvider(  XmlSerializerProvider src){
    super(src);
    QName rootName=src._rootNameLookup.getRootName();
    _rootNameLookup=new RootNameLookup(rootName,src._rootNameLookup);
  }
}
