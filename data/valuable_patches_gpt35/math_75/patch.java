public class tmp {
  public double getPct(  Object v){
    if(v instanceof Comparable<?>){
	Comparable<?> comparable = (Comparable<?>) v;
	return getCumPct(comparable);
    }	
    return 0.0;
  }
}
