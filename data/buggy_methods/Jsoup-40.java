public class tmp {
  public void DocumentType(  String name,  String publicId,  String systemId,  String baseUri){
    super(baseUri);
    Validate.notEmpty(name);
    attr("name",name);
    attr("publicId",publicId);
    attr("systemId",systemId);
  }
}
