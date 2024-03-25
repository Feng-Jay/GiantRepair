package mfix.tools;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Timer {
    private long _start = 0;
    private long _timeout = 0;

    public Timer(int min){
        _timeout += TimeUnit.MINUTES.toMillis(min);
        System.out.println("TIMEOUT : " + _timeout + "ms");
    }

    public long whenStart() {
        return _start;
    }

    public String start(){
        _start = System.currentTimeMillis();
        return new Date(_start).toString();
    }

    public boolean lowestRunTime(){
        return !((System.currentTimeMillis() - _start) < (_timeout/5));
    }

    public boolean timeout(){
        if((System.currentTimeMillis() - _start) > _timeout) {
            System.out.println("Timeout !");
            return true;
        }
        return false;
    }
}
