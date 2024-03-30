public class tmp {
  public final Object computeValue(  EvalContext context){
    Object arg1=args[0].computeValue(context);
    Object arg2=args[1].computeValue(context);
    if (arg1 == null || arg2 == null) {
      return Boolean.FALSE;
    }
    return compute(arg1,arg2) ? Boolean.TRUE : Boolean.FALSE;
  }
}
