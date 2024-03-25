package mfix.core.node.check;

import mfix.common.util.Utils;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.SName;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FuncSignature extends Members{
    private String _retType;
    private String _funcName;
    private List<Variable> _parameterList;

    private Node _node;

    private String _parameterNums;

    // should pass in a func decl node???
    public FuncSignature(int line, ASTNode node, String fileName){
        super(line);
        if(!(node instanceof MethodDeclaration)){
            return;
        }
        MethodDeclaration decl = (MethodDeclaration) node;
        _retType = decl.getReturnType2().toString();

        _funcName= decl.getName().toString();

        _parameterList = new ArrayList<>();
        List<SingleVariableDeclaration> parameters = decl.parameters();
        int counter = 0;
        for(SingleVariableDeclaration parameter: parameters){
            if(parameter.isVarargs()){
                _parameterNums = ">=:";
                continue;
            }
            String name = parameter.getName().getIdentifier();
            String type = parameter.getType().toString();
            SName name1 = new SName(fileName, line,line,parameter.getName());
            Variable tmp = new Variable(line,name, type, name1);
            counter++;
            _parameterList.add(tmp);
        }
        if(_parameterNums == null){
            _parameterNums = String.valueOf(counter);
        }else{
            _parameterNums = _parameterNums + counter;
        }
    }

    public String getRetType(){
        return _retType;
    }

    public String getFuncName(){
        return _funcName;
    }

    public List<Variable> getParameters(){
        return _parameterList;
    }

    public String getParameterNum(){
        return _parameterNums;
    }

    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if(!(obj instanceof FuncSignature)){
            return false;
        }
        FuncSignature func = (FuncSignature) obj;
        return Utils.safeStringEqual(this._retType, func._retType)
                && Utils.safeStringEqual(this._funcName, func._funcName)
                && this._parameterList.equals(((FuncSignature) obj)._parameterList)
                && Objects.equals(this.getLine(), func.getLine());
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.getLine(), this._retType, this._funcName, this._parameterList);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("FuncSignature: ");
        sb.append(_retType);
        sb.append(" ");
        sb.append(_funcName);
        sb.append("(");
        for(Variable var: _parameterList){
            sb.append(var.toString());
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
}
