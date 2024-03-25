package mfix.common.util;

public class Triple<T1, T2, T>  {

    private T1 first;
    private T2 second;
    private T third;

    public Triple() {
    }

    public Triple(T1 fst, T2 snd, T third){
        this.first = fst;
        this.second = snd;
        this.third = third;
    }

    public T1 getFirst(){
        return this.first;
    }

    public T2 getSecond(){
        return this.second;
    }

    public T getThird() {
        return this.third;
    }

    public void setFirst(T1 fst){
        this.first = fst;
    }

    public void setSecond(T2 snd){
        this.second = snd;
    }

    public void setThird(T third) {
        this.third = third;
    }

}
