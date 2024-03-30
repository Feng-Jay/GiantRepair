public class tmp {
  public double[] fit(  final Gaussian.Parametric param){
    final double[] guess=(new ParameterGuesser(getObservations())).guess();
    return fit(param,guess);
  }
}
