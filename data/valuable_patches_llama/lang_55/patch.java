public class tmp {
  public void stop(){
    if (this.runningState == STATE_RUNNING || this.runningState == STATE_SUSPENDED) {
      stopTime=System.currentTimeMillis();
      runningState=STATE_STOPPED;
    }
 else {
      throw new IllegalStateException("Stopwatch is not running. ");
    }
  }
}
