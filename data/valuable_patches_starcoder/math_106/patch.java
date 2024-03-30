public class tmp {
  public Fraction parse(  String source,  ParsePosition pos){
    Fraction ret=super.parse(source,pos);
    if (ret != null) {
      return ret;
    }
    int initialIndex=pos.getIndex();
    parseAndIgnoreWhitespace(source,pos);
    Number whole=getWholeFormat().parse(source,pos);
    if (whole == null) {
      pos.setIndex(initialIndex);
      return null;
    }
    parseAndIgnoreWhitespace(source,pos);
    Number num=getNumeratorFormat().parse(source,pos);
    if (num == null) {
      pos.setIndex(initialIndex);
      return null;
    }
    parseAndIgnoreWhitespace(source,pos);
    Number den=getDenominatorFormat().parse(source,pos);
    if (den == null) {
      pos.setIndex(initialIndex);
      return null;
    }
    if (whole.intValue() < 0 || num.intValue() < 0 || den.intValue() < 0) {
      pos.setIndex(initialIndex);
      return null;
    }
    int w=whole.intValue();
    int n=num.intValue();
    int d=den.intValue();
    return new Fraction(((Math.abs(w) * d) + n) * MathUtils.sign(w),d);
  }
}
