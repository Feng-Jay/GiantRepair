package mfix.core.node.check;

import mfix.common.util.Utils;
import mfix.core.node.ast.expr.SName;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConstructorNode extends Members{

    private String _funcName;

    private List<Variable> _parameterList;

    private String _parameterNums;

    public ConstructorNode(Integer line, ASTNode node, String fileName){
        super(line);
        if(!(node instanceof MethodDeclaration) || !((MethodDeclaration) node).isConstructor()){
            return;
        }
        MethodDeclaration decl = (MethodDeclaration) node;

        _funcName= decl.getName().getIdentifier();

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
            _parameterList.add(tmp);
            counter ++;
        }
        if(_parameterNums == null){
            _parameterNums = String.valueOf(counter);
        }else{
            _parameterNums = _parameterNums + counter;
        }
    }

    public String getFuncName(){return _funcName;}

    public List<Variable> getParameterList(){return _parameterList;}

    public String getParameterNums(){return _parameterNums;}

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("ConstructorNode:");
        sb.append(_funcName);
        sb.append("(");
        for(Variable var: _parameterList){
            sb.append(var.toString());
            sb.append(", ");
        }
        sb.append(")");
        return sb.toString();
    }
    @Override
    public boolean equals(Object obj){
        if(obj == null){
            return false;
        }
        if(!(obj instanceof ConstructorNode)){
            return false;
        }
        ConstructorNode func = (ConstructorNode) obj;
        return Utils.safeStringEqual(this._funcName, func._funcName)
                && this._parameterList.equals(((ConstructorNode) obj)._parameterList)
                && Objects.equals(this.getLine(), func.getLine());
    }

    @Override
    public int hashCode(){
        return Objects.hash(this.getLine(), this._funcName, this._parameterList);
    }

}
