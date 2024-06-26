public class tmp {
  public void atan2(  final double[] y,  final int yOffset,  final double[] x,  final int xOffset,  final double[] result,  final int resultOffset){
    double[] tmp1=new double[getSize()];
    multiply(x,xOffset,x,xOffset,tmp1,0);
    double[] tmp2=new double[getSize()];
    multiply(y,yOffset,y,yOffset,tmp2,0);
    add(tmp1,0,tmp2,0,tmp2,0);
    rootN(tmp2,0,2,tmp1,0);
    if (x[xOffset] >= 0) {
      add(tmp1,0,x,xOffset,tmp2,0);
      divide(y,yOffset,tmp2,0,tmp1,0);
      atan(tmp1,0,tmp2,0);
      for (int i=0; i < tmp2.length; ++i) {
        result[resultOffset + i]=2 * tmp2[i];
      }
    }
 else {
      subtract(tmp1,0,x,xOffset,tmp2,0);
      divide(y,yOffset,tmp2,0,tmp1,0);
      atan(tmp1,0,tmp2,0);
      result[resultOffset]=((tmp2[0] <= 0) ? -FastMath.PI : FastMath.PI) - 2 * tmp2[0];
      for (int i=1; i < tmp2.length; ++i) {
        result[resultOffset + i]=-2 * tmp2[i];
      }
    }
    if (Double.isNaN(result[resultOffset])) {
      result[resultOffset]=FastMath.atan2(y[yOffset],x[xOffset]);
    }
  }
}
