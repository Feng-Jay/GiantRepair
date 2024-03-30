public class tmp {
  public void addValue(  double value){
    sumImpl.increment(value);
    sumsqImpl.increment(value);
    minImpl.increment(value);
    maxImpl.increment(value);
    sumLogImpl.increment(value);
    secondMoment.increment(value);
    if (meanImpl != null) {
      meanImpl.increment(value);
    }
    if (varianceImpl != null) {
      varianceImpl.increment(value);
    }
    if (geoMeanImpl != null) {
      geoMeanImpl.increment(value);
    }
    n++;
  }
}
