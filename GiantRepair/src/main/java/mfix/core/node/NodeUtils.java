
package mfix.core.node;

import mfix.common.util.JavaFile;
import mfix.common.util.Utils;
import mfix.core.node.ast.LineRange;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.VarScope;
import mfix.core.node.ast.Variable;
import mfix.core.node.ast.expr.*;
import mfix.core.node.ast.stmt.EmptyStmt;
import mfix.core.node.ast.stmt.ExpressionStmt;
import org.eclipse.jdt.core.dom.*;

import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;



public class NodeUtils {

    public static Set<String> IGNORE_METHOD_INVOKE = new HashSet<String>(Arrays.asList("toString", "equals",
            "hashCode"));

    public static int parseTreeSize(Node node) {
        int size = 0;
        if (node != null) {
            if (node != null) {
                Queue<Node> nodes = new LinkedList<>();
                nodes.add(node);
                while (!nodes.isEmpty()) {
                    node = nodes.poll();
                    if (NodeUtils.isSimpleExpr(node)) {
                        size += 1;
                    } else {
                        nodes.addAll(node.getAllChildren());
                    }
                }
            }
        }
        return size;
    }

    public final static Set<String> primitives = new HashSet<>(Arrays.asList("char", "short", "int", "float", "double"));
    public static boolean matchType(String t1, String t2) {
        if (t1 == t2) return true;
        if (t1 == null) return t2 == null;
        if (t1.equals(t2)) return true;
        return primitives.contains(t1) && primitives.contains(t2);
    }

    public static String decorateMethodName(SName name) {
        return "MFIXTD_" + (name == null ? "" : name.getName());
    }

    public static String dedecorateMethodName(String name) {
        if (name == null) return null;
        if (name.startsWith("MFIXTD_")) {
            return name.substring("MFIXTD_".length());
        }
        return name;
    }

    public static String distillBasicType(MType type) {
        String s = type.toSrcString().toString();
        return distillBasicType(s);
    }

    public static String distillBasicType(String s) {
        if (s == null) return null;
        if (s.startsWith("java.lang.")) {
            s = s.substring(10/*java.lang.*/);
        }
        int index = s.indexOf('<');
        return index > 0 ? s.substring(0, index) : s;
    }

    public static boolean isMethodName(Node node) {
        Node parent = node.getParent();
        if (parent instanceof MethodInv) {
            MethodInv methodInv = (MethodInv) parent;
            return methodInv.getName() == node;
        } else if (parent instanceof SuperMethodInv) {
            SuperMethodInv methodInv = (SuperMethodInv) parent;
            return methodInv.getMethodName() == node;
        }
        return false;
    }

    public static boolean possibleClassName(String name) {
        return Character.isUpperCase(name.charAt(0));
    }

    /**
     * check whether the given node is a simple node,
     * which usually represents a one token
     * @param node
     * @return
     */
    public static boolean isSimpleExpr(Node node) {
        switch (node.getNodeType()) {
            case SNAME:
            case QNAME:
            case NUMBER:
            case INTLITERAL:
            case FLITERAL:
            case DLITERAL:
            case NULL:
            case ASSIGNOPERATOR:
            case POSTOPERATOR:
            case PREFIXOPERATOR:
            case INFIXOPERATOR:
            case TYPE:
            case SLITERAL:
            case THIS:
            case BLITERAL:
            case CLITERAL:
            case TLITERAL:
                return true;
            default:
        }
        return false;
    }

    /**
     * check whether the source code of {@code node} and {@code other} can match
     * each other or not according to existing matching result {@code matchedStrings}.
     * If they can match each other, the mapping relation will be added to both
     * {@code matchedNode} and {@code matchedStrings}
     *
     * @param node           : source node to match
     * @param other          : target node to match
     * @param matchedNode    : already matched nodes
     * @param matchedStrings : already matched strings
     * @return : true if they can match each other, otherwise false
     */
    public static boolean matchSameNodeType(Node node, Node other, Map<Node, Node> matchedNode,
                                            Map<String, String> matchedStrings) {
        if (Utils.checkCompatiblePut(node.toString(), other.toString(), matchedStrings)) {
            matchedNode.put(node, other);
            return true;
        }
        return false;
    }

    public static boolean considerNode(Node node) {
        // avoid inserting empty statement
        if (node instanceof EmptyStmt) {
            return false;
        }
        // avoid inserting print or logging statement
        if (node instanceof ExpressionStmt) {
            ExpressionStmt expStmt = (ExpressionStmt) node;
            if (expStmt.getExpression() instanceof MethodInv) {
                String str = expStmt.toSrcString().toString();
                if (str.startsWith("System.out.") || str.startsWith("System.exit")
                        || str.startsWith("System.err") || str.startsWith("System.gc")
                        || str.startsWith("Log") || str.startsWith("LevelLogger")
                        || str.startsWith("Thread.currentThread()")) {
                    return false;
                }
            }
        }
        return true;
    }

    public static String getDefaultValue(String type){
        if (type == null) return null;
        switch(type){
            case "?":
                return "";
            case "Boolean":
            case "boolean":
                return "false";
            case "Short":
            case "short":
            case "Integer":
            case "int":
                return "0";
            case "Float":
            case "float":
                return "0f";
            case "Double":
            case "double":
                return "0d";
            case "Long":
            case "long":
                return "0l";
            case "Character":
            case "char":
                return "' '";
            default:
                return "null";
        }

    }

    public static boolean isLegalVar(String var) {
        String[] strings = var.split("\\.");
        Pattern p = Pattern.compile("[\\w|_][\\w|\\d|_]*(\\[.*\\])?");
        for (String s : strings) {
            if (!p.matcher(s).matches()) {
                return false;
            }
        }
        return true;
    }

    public static Type parseExprType(Expr left, String operator, Expr right) {
        if (left == null) {
            return parsePreExprType(right, operator);
        }

        if (right == null) {
            return parsePostExprType(left, operator);
        }

        AST ast = AST.newAST(AST.JLS8);
        switch (operator) {
            case "*":
            case "/":
            case "+":
            case "-":
                Type type = union(left.getType(), right.getType());
                if (type == null) {
                    type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
                }
                return type;
            case "%":
            case "<<":
            case ">>":
            case ">>>":
            case "^":
            case "&":
            case "|":
                return ast.newPrimitiveType(PrimitiveType.INT);
            case "<":
            case ">":
            case "<=":
            case ">=":
            case "==":
            case "!=":
            case "&&":
            case "||":
                return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            default:
                return null;
        }
    }

    private static Type union(Type ty1, Type ty2) {
        if (ty1 == null) {
            return ty2;
        } else if (ty2 == null) {
            return ty1;
        }

        if (!ty1.isPrimitiveType() || !ty2.isPrimitiveType()) {
            return null;
        }

        String ty1String = ty1.toString().toLowerCase().replace("integer", "int");
        String ty2String = ty2.toString().toLowerCase().replace("integer", "int");

        AST ast = AST.newAST(AST.JLS8);
        if (ty1String.equals("double") || ty2String.equals("double")) {

            return ast.newPrimitiveType(PrimitiveType.DOUBLE);

        } else if (ty1String.equals("float") || ty2String.equals("float")) {

            return ast.newPrimitiveType(PrimitiveType.FLOAT);

        } else if (ty1String.equals("long") || ty2String.equals("long")) {

            return ast.newPrimitiveType(PrimitiveType.LONG);

        } else if (ty1String.equals("int") || ty2String.equals("int")) {

            return ast.newPrimitiveType(PrimitiveType.INT);

        } else if (ty1String.equals("short") || ty2String.equals("short")) {

            return ast.newPrimitiveType(PrimitiveType.SHORT);

        } else {

            return ast.newPrimitiveType(PrimitiveType.BYTE);

        }

    }

    private static Type parsePostExprType(Expr expr, String operator) {
        // ++/--
        AST ast = AST.newAST(AST.JLS8);
        return ast.newPrimitiveType(PrimitiveType.INT);
    }

    private static Type parsePreExprType(Expr expr, String operator) {
        AST ast = AST.newAST(AST.JLS8);
        switch (operator) {
            case "++":
            case "--":
                return ast.newPrimitiveType(PrimitiveType.INT);
            case "+":
            case "-":
                return expr.getType();
            case "~":
            case "!":
                return ast.newPrimitiveType(PrimitiveType.BOOLEAN);
            default:
                return null;
        }
    }

    @Deprecated
    public static Map<Integer, Set<String>> getUsableVarTypes(String file) {
        CompilationUnit unit = JavaFile.genAST(file);
        VariableVisitor variableVisitor = new VariableVisitor(unit);
        unit.accept(variableVisitor);
        return variableVisitor.getVars();
    }

    public static Map<Integer, VarScope> getUsableVariables(String file) {
        CompilationUnit unit = JavaFile.genAST(file);
        NewVariableVisitor variableVisitor = new NewVariableVisitor(unit);
        unit.accept(variableVisitor);
        return variableVisitor.getVars();
    }

}


class VariableVisitor extends ASTVisitor {
    private MethodVisitor _methodVisitor = new MethodVisitor();
    private Map<Integer, Set<String>> _vars = new HashMap<>();
    private Set<String> _fields = new HashSet<>();
    private CompilationUnit _unit;

    public VariableVisitor(CompilationUnit unit) {
        _unit = unit;
    }

    public boolean visit(FieldDeclaration node) {
        for (Object object : node.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
            _fields.add(vdf.getName().toString());
        }
        return true;
    }

    public Map<Integer, Set<String>> getVars() {
        for (Entry<Integer, Set<String>> entry : _vars.entrySet()) {
            entry.getValue().addAll(_fields);
        }
        return _vars;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int start = _unit.getLineNumber(node.getStartPosition());
        Set<String> localVars = new HashSet<>();
        for (Object object : node.parameters()) {
            SingleVariableDeclaration svd = (SingleVariableDeclaration) object;
            localVars.add(svd.getName().toString());
        }

        if (node.getBody() != null) {
            _methodVisitor.reset();
            node.getBody().accept(_methodVisitor);
            localVars.addAll(_methodVisitor.getVars());
        }
        _vars.put(start, localVars);
        return true;
    }

    class MethodVisitor extends ASTVisitor {

        private Set<String> vars = new HashSet<>();

        public void reset() {
            vars.clear();
        }

        public Set<String> getVars() {
            return vars;
        }

        public boolean visit(VariableDeclarationStatement node) {
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                vars.add(vdf.getName().getFullyQualifiedName());
            }
            return true;
        }

        public boolean visit(VariableDeclarationExpression node) {
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                vars.add(vdf.getName().getFullyQualifiedName());
            }
            return true;
        }

        public boolean visit(SingleVariableDeclaration node) {
            vars.add(node.getName().getFullyQualifiedName());
            return true;
        }

    }

}

class NewVariableVisitor extends ASTVisitor {
    private MethodVisitor _methodVisitor = new MethodVisitor();
    private Map<Integer, VarScope> _vars = new HashMap<>();
    private Set<Variable> _fields = new HashSet<>();
    private CompilationUnit _unit;

    public NewVariableVisitor(CompilationUnit unit) {
        _unit = unit;
    }

    public boolean visit(FieldDeclaration node) {
        String type = node.getType().toString();
        for (Object object : node.fragments()) {
            VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
            _fields.add(new Variable(vdf.getName().getIdentifier(), type));
        }
        return true;
    }

    public Map<Integer, VarScope> getVars() {
        for (Entry<Integer, VarScope> entry : _vars.entrySet()) {
            entry.getValue().setGlobalVars(_fields);
        }
        return _vars;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        int start = _unit.getLineNumber(node.getStartPosition());
        int end = _unit.getLineNumber(node.getStartPosition() + node.getLength());
        VarScope scope = new VarScope();
        _methodVisitor.reset(scope, start, end);
        node.accept(_methodVisitor);
        _vars.put(start, scope);
        return true;
    }

    class MethodVisitor extends ASTVisitor {

        private VarScope _scope;
        private int _end;
        public void reset(VarScope scope, int start, int end) {
            _scope = scope;
            _end = end;
        }

        public boolean visit(VariableDeclarationStatement node) {
            String type = node.getType().toString();
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                _scope.addLocalVar(new Variable(vdf.getName().getIdentifier(), type), new LineRange(start, end));
            }
            return true;
        }

        public boolean visit(VariableDeclarationExpression node) {
            String type = node.getType().toString();
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            for (Object o : node.fragments()) {
                VariableDeclarationFragment vdf = (VariableDeclarationFragment) o;
                _scope.addLocalVar(new Variable(vdf.getName().getIdentifier(), type), new LineRange(start, end));
            }
            return true;
        }

        public boolean visit(SingleVariableDeclaration node) {
            int start = _unit.getLineNumber(node.getStartPosition());
            int end = getParentEnd(node);
            String type = node.getType().toString();
            _scope.addLocalVar(new Variable(node.getName().getIdentifier(), type), new LineRange(start, end));
            return true;
        }

        private int getParentEnd(ASTNode node) {
            while(node != null) {
                if (node instanceof MethodDeclaration || node instanceof Block
                        || node instanceof IfStatement || node instanceof WhileStatement
                        || node instanceof ForStatement || node instanceof EnhancedForStatement
                        || node instanceof DoStatement) {
                    return _unit.getLineNumber(node.getStartPosition() + node.getLength());
                }
                node = node.getParent();
            }
            return _end;
        }

    }

}
