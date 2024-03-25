package mfix.core.node.check;

import java.util.ArrayList;
import java.util.List;

public class Tracer {
    public List<String> _mutationTrace;

    public Tracer(){
        _mutationTrace = new ArrayList<>();
    }

    public void addTrace(String trace){
        _mutationTrace.add(trace);
    }

    public void addAllTrace(List<String> traces){
        _mutationTrace.addAll(traces);
    }
    public List<String> getTrace(){
        return _mutationTrace;
    }

}
