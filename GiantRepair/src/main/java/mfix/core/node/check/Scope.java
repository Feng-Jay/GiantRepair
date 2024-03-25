package mfix.core.node.check;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
This class is used to store static analysis' outcome
to store cur-method, cur-class, import-class, cur-directory's content.
 */
public class Scope {

    private String _className;

    private Scope _parent;

    private final List<Scope> _childScopes;

    private Map<String, String> _type2SuperClass;
    private Map<String, List<Variable>> _type2vars; // type to variable

    private Map<String, List<Variable>> _type2StaticVars;
    private Map<String, List<FuncSignature>> _type2funcs; // type to function

    private Map<String, List<FuncSignature>> _type2StaticFuncs;

    private List<Variable> _vars;

    private List<Variable> _staticVars;
    private List<FuncSignature> _funcs;
    private List<FuncSignature> _staticFuncs;
    private List<ConstructorNode> _constructors;

    private List<String> _methodInvocations;

    private int _startLine;

    private int _endLine;

    public Scope(Scope parent, String name){
        _className = name;
        _parent = parent;
        _childScopes = new ArrayList<>();

        _type2vars   = new HashMap<>();
        _type2StaticVars = new HashMap<>();

        _type2funcs  = new HashMap<>();
        _type2StaticFuncs = new HashMap<>();
        _type2SuperClass = new HashMap<>();

        _vars = new ArrayList<>();
        _staticVars = new ArrayList<>();

        _funcs= new ArrayList<>();
        _staticFuncs = new ArrayList<>();

        _constructors = new ArrayList<>();

        _methodInvocations = new ArrayList<>();
    }

    public void setStartLine(int startLine){_startLine = startLine;}

    public int getStartLine(){return _startLine;}

    public void setEndLine(int endLine){_endLine = endLine;}

    public int getEndLine(){return _endLine;}

    public void setClassName(String name){_className = name;}
    public String getClassName(){
        return _className;
    }

    public Scope getParent(){
        return _parent;
    }

    public void setParent(Scope parent){_parent = parent;}

    public List<Scope> getChildScopes(){return _childScopes;}

    public void addChildScope(Scope child){_childScopes.add(child);}

    public void addVar(Variable var){
        _vars.add(var);
        if(_type2vars.containsKey(var.getType())){
            _type2vars.get(var.getType()).add(var);
        }else{
            List<Variable> vars = new ArrayList<>();
            vars.add(var);
            _type2vars.put(var.getType(), vars);
        }
    }
    public void setVars(List<Variable> vars){_vars = vars;}
    public List<Variable> getVars(){
        return _vars;
    }

    public void addStaticVar(Variable var){
        _staticVars.add(var);
        if(_type2StaticVars.containsKey(var.getType())){
            _type2StaticVars.get(var.getType()).add(var);
        }else{
            List<Variable> vars = new ArrayList<>();
            vars.add(var);
            _type2StaticVars.put(var.getType(), vars);
        }
    }
    public void setStaticVars(List<Variable> vars){ _staticVars = vars;}
    public List<Variable> getStaticVars(){return _staticVars;}

    public void setType2SuperClass(Map<String, String> type2SuperClass){_type2SuperClass = type2SuperClass;}
    public Map<String, String> getType2SuperClass(){return _type2SuperClass;}

    public void setType2Vars(Map<String, List<Variable>> type2Vars){_type2vars = type2Vars;}
    public Map<String, List<Variable>> getType2Vars(){return _type2vars;}

    public void setType2StaticVars(Map<String, List<Variable>> type2Vars){_type2StaticVars = type2Vars;}
    public Map<String, List<Variable>> getType2StaticVars(){return _type2StaticVars;}

    public List<Variable> getVarsByType(String type){
        return _type2vars.get(type);
    }

    public void addFunction(FuncSignature func){
        _funcs.add(func);
        if(_type2funcs.containsKey(func.getRetType())){
            _type2funcs.get(func.getRetType()).add(func);
        }else{
            List<FuncSignature> funcsList = new ArrayList<>();
            funcsList.add(func);
            _type2funcs.put(func.getRetType(), funcsList);
        }
    }
    public void setFunctions(List<FuncSignature> funcs){_funcs = funcs;}
    public List<FuncSignature> getFuncs(){return _funcs;}

    public void addStaticFunc(FuncSignature func){
        _staticFuncs.add(func);
        if(_type2StaticFuncs.containsKey(func.getRetType())){
            _type2StaticFuncs.get(func.getRetType()).add(func);
        }else{
            List<FuncSignature> funcsList = new ArrayList<>();
            funcsList.add(func);
            _type2StaticFuncs.put(func.getRetType(), funcsList);
        }
    }
    public void setStaticFuncs(List<FuncSignature> funcs){_staticFuncs = funcs;}
    public List<FuncSignature> getStaticFuncs(){return _staticFuncs;}

    public void setType2Funcs(Map<String, List<FuncSignature>> type2Funcs){_type2funcs = type2Funcs;}
    public Map<String, List<FuncSignature>> getType2Funcs(){return _type2funcs;}

    public void setType2StaticFuncs(Map<String, List<FuncSignature>> type2Funcs){_type2StaticFuncs = type2Funcs;}
    public Map<String, List<FuncSignature>> getType2StaticFuncs(){return _type2StaticFuncs;}

    public List<FuncSignature> getFuncsByType(String type){return _type2funcs.get(type);}

    public void addConstructor(ConstructorNode constructor){
        _constructors.add(constructor);
    }

    public void setConstructors(List<ConstructorNode> constructors){_constructors = constructors;}

    public List<ConstructorNode> getConstructors(){return _constructors;}

   public Variable containsVar(String name){
        for(Variable var: _vars){
            if(var.getName().equals(name)){
                return var;
            }
        }
        return null;
   }

   public Variable containsStaticVar(String name){
        for(Variable var: _staticVars){
            if(var.getName().equals(name)){
                return var;
            }
        }
        return null;
   }

   public void setMethodInvocations(List<String> methodInvocations){_methodInvocations = methodInvocations;}

    public List<String> getMethodInvocations(){return _methodInvocations;}
}
