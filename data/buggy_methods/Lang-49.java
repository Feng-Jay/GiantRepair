public class tmp {
  public Fraction reduce(){
    int gcd=greatestCommonDivisor(Math.abs(numerator),denominator);
    if (gcd == 1) {
      return this;
    }
    return Fraction.getFraction(numerator / gcd,denominator / gcd);
  }
}
