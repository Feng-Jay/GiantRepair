public class tmp {
  public double solve(  final UnivariateRealFunction f,  final double min,  final double max,  final double initial) throws MaxIterationsExceededException, FunctionEvaluationException {
    clearResult();
    verifySequence(min,initial,max);
    double yInitial=f.value(initial);
    if (Math.abs(yInitial) <= functionValueAccuracy) {
      setResult(initial,0);
      return result;
    }
    double yMin=f.value(min);
    if (Math.abs(yMin) <= functionValueAccuracy) {
      setResult(min,0);
      return result;
    }
    if (yInitial * yMin < 0) {
      return solve(f,min,initial,min,yMin,initial,yInitial);
    }
    double yMax=f.value(max);
    if (Math.abs(yMax) <= functionValueAccuracy) {
      setResult(max,0);
      return result;
    }
    if (yInitial * yMax < 0) {
      return solve(f,initial,max,initial,yInitial,max,yMax);
    }
    if (yMin * yMax > 0) {
      throw MathRuntimeException.createIllegalArgumentException(NON_BRACKETING_MESSAGE,min,max,yMin,yMax);
    }
    return solve(f,min,yMin,max,yMax,initial,yInitial);
  }
}
