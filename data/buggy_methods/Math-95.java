public class tmp {
  protected double getInitialDomain(  double p){
    double ret;
    double d=getDenominatorDegreesOfFreedom();
    ret=d / (d - 2.0);
    return ret;
  }
}
