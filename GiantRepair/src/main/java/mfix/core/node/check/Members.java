package mfix.core.node.check;

public abstract class Members {
    private Integer _line;

    public Members(int line){
        _line = line;
    }

    public Integer getLine(){
        return _line;
    }


}
