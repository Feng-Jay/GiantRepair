public class tmp {
  public StringBuilder getGenericSignature(  StringBuilder sb){
    sb.append('L');
    _classSignature(_class,sb,false);
    sb.append('<');
    sb=_referencedType.getGenericSignature(sb);
    sb.append('>');
    sb.append(';');
    return sb;
  }
}
