public class tmp {
  protected VectorialPointValuePair doOptimize() throws FunctionEvaluationException, OptimizationException, IllegalArgumentException {
    solvedCols=Math.min(rows,cols);
    diagR=new double[cols];
    jacNorm=new double[cols];
    beta=new double[cols];
    permutation=new int[cols];
    lmDir=new double[cols];
    double delta=0;
    double xNorm=0;
    double[] diag=new double[cols];
    double[] oldX=new double[cols];
    double[] oldRes=new double[rows];
    double[] work1=new double[cols];
    double[] work2=new double[cols];
    double[] work3=new double[cols];
    updateResidualsAndCost();
    lmPar=0;
    boolean firstIteration=true;
    VectorialPointValuePair current=new VectorialPointValuePair(point,objective);
    while (true) {
      incrementIterationsCounter();
      VectorialPointValuePair previous=current;
      updateJacobian();
      qrDecomposition();
      qTy(residuals);
      for (int k=0; k < solvedCols; ++k) {
        int pk=permutation[k];
        jacobian[k][pk]=diagR[pk];
      }
      if (firstIteration) {
        xNorm=0;
        for (int k=0; k < cols; ++k) {
          double dk=jacNorm[k];
          if (dk == 0) {
            dk=1.0;
          }
          double xk=dk * point[k];
          xNorm+=xk * xk;
          diag[k]=dk;
        }
        xNorm=Math.sqrt(xNorm);
        delta=(xNorm == 0) ? initialStepBoundFactor : (initialStepBoundFactor * xNorm);
      }
      double maxCosine=0;
      if (cost != 0) {
        for (int j=0; j < solvedCols; ++j) {
          int pj=permutation[j];
          double s=jacNorm[pj];
          if (s != 0) {
            double sum=0;
            for (int i=0; i <= j; ++i) {
              sum+=jacobian[i][pj] * residuals[i];
            }
            maxCosine=Math.max(maxCosine,Math.abs(sum) / (s * cost));
          }
        }
      }
      if (maxCosine <= orthoTolerance) {
        return current;
      }
      for (int j=0; j < cols; ++j) {
        diag[j]=Math.max(diag[j],jacNorm[j]);
      }
      for (double ratio=0; ratio < 1.0e-4; ) {
        for (int j=0; j < solvedCols; ++j) {
          int pj=permutation[j];
          oldX[pj]=point[pj];
        }
        double previousCost=cost;
        double[] tmpVec=residuals;
        residuals=oldRes;
        oldRes=tmpVec;
        determineLMParameter(oldRes,delta,diag,work1,work2,work3);
        double lmNorm=0;
        for (int j=0; j < solvedCols; ++j) {
          int pj=permutation[j];
          lmDir[pj]=-lmDir[pj];
          point[pj]=oldX[pj] + lmDir[pj];
          double s=diag[pj] * lmDir[pj];
          lmNorm+=s * s;
        }
        lmNorm=Math.sqrt(lmNorm);
        if (firstIteration) {
          delta=Math.min(delta,lmNorm);
        }
        updateResidualsAndCost();
        current=new VectorialPointValuePair(point,objective);
        double actRed=-1.0;
        if (0.1 * cost < previousCost) {
          double r=cost / previousCost;
          actRed=1.0 - r * r;
        }
        for (int j=0; j < solvedCols; ++j) {
          int pj=permutation[j];
          double dirJ=lmDir[pj];
          work1[j]=0;
          for (int i=0; i <= j; ++i) {
            work1[i]+=jacobian[i][pj] * dirJ;
          }
        }
        double coeff1=0;
        for (int j=0; j < solvedCols; ++j) {
          coeff1+=work1[j] * work1[j];
        }
        double pc2=previousCost * previousCost;
        coeff1=coeff1 / pc2;
        double coeff2=lmPar * lmNorm * lmNorm / pc2;
        double preRed=coeff1 + 2 * coeff2;
        double dirDer=-(coeff1 + coeff2);
        ratio=(preRed == 0) ? 0 : (actRed / preRed);
        if (ratio <= 0.25) {
          double tmp=(actRed < 0) ? (0.5 * dirDer / (dirDer + 0.5 * actRed)) : 0.5;
          if ((0.1 * cost >= previousCost) || (tmp < 0.1)) {
            tmp=0.1;
          }
          delta=tmp * Math.min(delta,10.0 * lmNorm);
          lmPar/=tmp;
        }
 else         if ((lmPar == 0) || (ratio >= 0.75)) {
          delta=2 * lmNorm;
          lmPar*=0.5;
        }
        if (ratio >= 1.0e-4) {
          firstIteration=false;
          xNorm=0;
          for (int k=0; k < cols; ++k) {
            double xK=diag[k] * point[k];
            xNorm+=xK * xK;
          }
          xNorm=Math.sqrt(xNorm);
        }
 else {
          cost=previousCost;
          for (int j=0; j < solvedCols; ++j) {
            int pj=permutation[j];
            point[pj]=oldX[pj];
          }
          tmpVec=residuals;
          residuals=oldRes;
          oldRes=tmpVec;
        }
        if (checker == null) {
          if (((Math.abs(actRed) <= costRelativeTolerance) && (preRed <= costRelativeTolerance) && (ratio <= 2.0)) || (delta <= parRelativeTolerance * xNorm)) {
            return current;
          }
        }
 else {
          if (checker.converged(getIterations(),previous,current)) {
            return current;
          }
        }
        if ((Math.abs(actRed) <= 2.2204e-16) && (preRed <= 2.2204e-16) && (ratio <= 2.0)) {
          throw new OptimizationException(LocalizedFormats.TOO_SMALL_COST_RELATIVE_TOLERANCE,costRelativeTolerance);
        }
 else         if (delta <= 2.2204e-16 * xNorm) {
          throw new OptimizationException(LocalizedFormats.TOO_SMALL_PARAMETERS_RELATIVE_TOLERANCE,parRelativeTolerance);
        }
 else         if (maxCosine <= 2.2204e-16) {
          throw new OptimizationException(LocalizedFormats.TOO_SMALL_ORTHOGONALITY_TOLERANCE,orthoTolerance);
        }
      }
    }
  }
}
