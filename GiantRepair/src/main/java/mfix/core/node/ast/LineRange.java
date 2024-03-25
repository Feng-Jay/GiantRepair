

package mfix.core.node.ast;

public class LineRange {

    private int _start;
    private int _end;

    public LineRange(int start, int end) {
        _start = start;
        _end = end;
    }

    public int getStart() {
        return _start;
    }

    public int getEnd() {
        return _end;
    }

    public boolean contains(int line) {
        return _start <= line && line <= _end;
    }

    @Override
    public int hashCode() {
        return _start + _end;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof LineRange)) {
            return false;
        }

        LineRange range = (LineRange) obj;
        return _start == range._start && _end == range._end;
    }
}
