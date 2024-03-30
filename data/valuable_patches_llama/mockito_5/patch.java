public class tmp {
  public void verify(  VerificationData data){
    AssertionError error=null;
    timer.start();
    while (timer.isCounting()) {
      try {
        delegate.verify(data);
        if (returnOnSuccess) {
          return;
        }
 else {
          error=null;
        }
      }
 catch (      org.mockito.exceptions.verification.junit.ArgumentsAreDifferent e) {
        error=handleVerifyException(e);
      }
catch (      MockitoAssertionError e) {
        error=handleVerifyException(e);
      }
    }
    if (error != null) {
      throw error;
    }
  }
}
