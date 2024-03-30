public class tmp {
  public double[] fit(){
    final double[] guess=(new ParameterGuesser(getObservations())).guess();
    Gaussian.Parametric gaussian=new Gaussian.Parametric();
    return gaussian.fit(guess);
  }
}
