public class tmp {
  public void addValue(  double value){
    sumImpl.increment(value);
    sumsqImpl.increment(value);
    minImpl.increment(value);
    maxImpl.increment(value);
    sumLogImpl.increment(value);
    secondMoment.increment(value);
    if (!(meanImpl instanceof Mean)) {
      meanImpl.increment(value);
    }
    if (!(varianceImpl instanceof Variance)) {
      varianceImpl.increment(value);
    }
    if (!(geoMeanImpl instanceof GeometricMean)) {
      geoMeanImpl.increment(value);
    }
    n++;
  }
}
