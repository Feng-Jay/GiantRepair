public class tmp {
  public static String abbreviate(  String str,  int lower,  int upper,  String appendToEnd){
    if (str == null) {
      return null;
    }
    if (lower < 0 || lower > str.length()) {
      lower=0;
    }
    if (upper < lower || upper > str.length()) {
      upper=str.length();
    }
    StringBuffer result=new StringBuffer();
    int index=StringUtils.indexOf(str," ",lower);
    if (index == -1) {
      result.append(str.substring(0,upper));
    }
 else     if (index > upper) {
      result.append(str.substring(0,upper));
    }
 else {
      result.append(str.substring(0,index));
    }
    if (upper != str.length()) {
      result.append(StringUtils.defaultString(appendToEnd));
    }
    return result.toString();
  }
}
