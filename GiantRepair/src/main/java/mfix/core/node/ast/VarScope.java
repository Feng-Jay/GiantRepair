

package mfix.core.node.ast;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VarScope {

    private Set<Variable> _globalVars;
    private Map<Variable, Set<LineRange>> _localVars;
    private transient Set<Variable> _newVars;
    private boolean _disable = false;

    public VarScope() {
        _globalVars = new HashSet<>();
        _localVars = new HashMap<>();
    }

    public void setDisable(boolean disable) {
        _disable = disable;
    }

    public void reset(Set<Variable> newVars) {
        _newVars = newVars;
    }

    public void setGlobalVars(final Set<Variable> globalVar) {
        _globalVars = globalVar;
    }

    public void addGlobalVar(Variable var) {
        _globalVars.add(var);
    }

    public void addGlobalVar(final String name, final String type) {
        addGlobalVar(new Variable(name, type));
    }

    public void addLocalVar(Variable variable, LineRange lineRange) {
        Set<LineRange> ranges = _localVars.get(variable);
        if (ranges == null) {
            ranges = new HashSet<>();
            _localVars.put(variable, ranges);
        }
        ranges.add(lineRange);
    }

    public void addLocalVar(final String name, final String type, final int start, final int end) {
        Variable variable = new Variable(name, type);
        LineRange range = new LineRange(start, end);
        addLocalVar(variable, range);
    }

    public boolean canUse(final String name, final String type, final int line) {
        if (_disable) return true;
        Variable variable = new Variable(name, type);
        Set<LineRange> ranges = _localVars.get(variable);
        if (ranges != null) {
            for (LineRange r : ranges) {
                if (r.getStart() < line/*r.contains(line)*/) {
                    return true;
                }
            }
        }
        if (_newVars != null) {
            if (_newVars.contains(variable)) {
                return true;
            }
        }
        return _globalVars.contains(variable);
    }

}

