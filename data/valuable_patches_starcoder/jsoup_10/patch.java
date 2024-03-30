public class tmp {
  public String absUrl(  String attributeKey){
    Validate.notEmpty(attributeKey);
    String relUrl=attr(attributeKey);
    if (!hasAttr(attributeKey)) {
      return "";
    }
 else {
      URL base;
      try {
        base=new URL(baseUri);
        if (relUrl.startsWith("?")) {
          URL abs=new URL(base,base.getPath() + relUrl);
          return abs.toExternalForm();
        }
        URL abs=new URL(base,relUrl);
        return abs.toExternalForm();
      }
 catch (      MalformedURLException e) {
        return "";
      }
    }
  }
}
