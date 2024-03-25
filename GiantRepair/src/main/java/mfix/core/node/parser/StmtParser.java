package mfix.core.node.parser;

import mfix.common.util.LevelLogger;
import mfix.core.node.NodeUtils;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.expr.Comment;
import mfix.core.node.ast.expr.MethodRef;
import mfix.core.node.ast.expr.*;
import mfix.core.node.ast.stmt.*;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StmtParser {

    private CompilationUnit _cunit;
    private String _fileName;

    public StmtParser(){

    }

    public StmtParser setCompilationUnit(String fileName, CompilationUnit unit){
        _cunit = unit;
        _fileName = fileName;
        return this;
    }

    private MethDecl visit(MethodDeclaration node, Node structure){
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethDecl methDecl = new MethDecl(_fileName, start, end, node);
        List<String> modifiers = new ArrayList<>(5);
        for(Object object: node.modifiers()) {
            modifiers.add(object.toString());
        }
        methDecl.setModifiers(modifiers);
        if(node.getReturnType2() != null) {
            Type type = typeFromBinding(node.getAST(), node.getReturnType2().resolveBinding());
            if (type == null || type instanceof WildcardType) {
                methDecl.setRetType(node.getReturnType2());
            } else {
                methDecl.setRetType(type);
            }
        }
        SName name = (SName) process(node.getName(), structure);
        name.setParent(methDecl);
        methDecl.setName(name);
        List<Expr> params = new ArrayList<>(11);
        for (Object arg : node.parameters()) {
            Expr param = (Expr) process((ASTNode) arg, structure);
            param.setParent(methDecl);
            params.add(param);
        }
        methDecl.setArguments(params);

        List<String> throwTypes = new ArrayList<>(7);
        for(Object object : node.thrownExceptionTypes()) {
            Type throwType = typeFromBinding(node.getAST(), ((Type) object).resolveBinding());
            if(throwType == null || throwType instanceof WildcardType) {
                throwTypes.add(object.toString());
            } else {
                throwTypes.add(throwType.toString());
            }
        }

        methDecl.setThrows(throwTypes);

        Block body = node.getBody();
        if (body != null) {
            Blk bBlk = (Blk) process(body, structure);
            bBlk.setStmtParent(methDecl);
            methDecl.setBody(bBlk);
        }
        return methDecl;
    }

    /*Begin the stmt parser*/
    private AssertStmt visit(AssertStatement node, Node structure) {
        int start = _cunit.getLineNumber(node.getStartPosition());
        int end = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AssertStmt assertStmt = new AssertStmt(_fileName, start, end, node);
        Expression expression = node.getExpression();
        ParenthesiszedExpr expr;
        if (expression instanceof ParenthesizedExpression) {
            expr = (ParenthesiszedExpr) process(expression, structure);
        } else {
            expr = new ParenthesiszedExpr(_fileName, start, end, expression);
            Expr e = (Expr) process(expression, structure);
            e.setParent(expr);
            expr.setExpr(e);
        }
        expr.setType(typeFromBinding(expression.getAST(), expression.resolveTypeBinding()));
        expr.setParent(assertStmt);
        assertStmt.setExpression(expr);

        if (node.getMessage() != null) {
            Expr message = (Expr) process(node.getMessage(), structure);
            message.setParent(assertStmt);
            assertStmt.setMessage(message);
        }

        assertStmt.setControldependency(structure);
        return assertStmt;
    }

    private BreakStmt visit(BreakStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BreakStmt breakStmt = new BreakStmt(_fileName, startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(_fileName, startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(breakStmt);
            breakStmt.setIdentifier(sName);
//            sName.setDataDependency(scope.getDefines(sName.getName()));
        }
        breakStmt.setControldependency(structure);
        return breakStmt;
    }

    private Blk visit(Block node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Blk blk = new Blk(_fileName, startLine, endLine, node);
        List<Stmt> stmts = new ArrayList<>();
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object, structure);
            stmt.setStmtParent(blk);
            stmts.add(stmt);
            if(stmt instanceof IfStmt){
                Stmt elsePart = ((IfStmt) stmt).getElse();
                if(elsePart != null) {
                    elsePart.setStmtParent(blk);
                    stmts.add(elsePart);
                }
            }

        }
        blk.setStatement(stmts);
        return blk;
    }

    private ConstructorInv visit(ConstructorInvocation node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConstructorInv constructorInv = new ConstructorInv(_fileName, startLine, endLine, node);
        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, structure);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(constructorInv);
        constructorInv.setArguments(exprList);
        constructorInv.setControldependency(structure);
        return constructorInv;
    }

    private ContinueStmt visit(ContinueStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ContinueStmt continueStmt = new ContinueStmt(_fileName, startLine, endLine, node);
        if (node.getLabel() != null) {
            SName sName = new SName(_fileName, startLine, endLine, node.getLabel());
            sName.setName(node.getLabel().getFullyQualifiedName());
            sName.setParent(continueStmt);
            continueStmt.setIdentifier(sName);
//            sName.setDataDependency(scope.getDefines(sName.getName()));
        }
        continueStmt.setControldependency(structure);
        return continueStmt;
    }

    private DoStmt visit(DoStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        DoStmt doStmt = new DoStmt(_fileName, startLine, endLine, node);
        doStmt.setControldependency(structure);

        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(doStmt);
        doStmt.setExpression(expression);

        Stmt stmt = wrapBlock(node.getBody(), expression);
        stmt.setStmtParent(doStmt);
        doStmt.setBody(stmt);

        return doStmt;
    }

    private EmptyStmt visit(EmptyStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EmptyStmt emptyStmt = new EmptyStmt(_fileName, startLine, endLine, node);
        emptyStmt.setControldependency(structure);
        return emptyStmt;
    }

    private EnhancedForStmt visit(EnhancedForStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        EnhancedForStmt enhancedForStmt = new EnhancedForStmt(_fileName, startLine, endLine, node);
        enhancedForStmt.setDataDependency(structure);

        Svd svd = (Svd) process(node.getParameter(), structure);
        svd.setParent(enhancedForStmt);
        enhancedForStmt.setParameter(svd);

        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(enhancedForStmt);
        enhancedForStmt.setExpression(expression);

        Stmt body = wrapBlock(node.getBody(), expression);
        body.setStmtParent(enhancedForStmt);
        enhancedForStmt.setBody(body);

        return enhancedForStmt;
    }

    private ExpressionStmt visit(ExpressionStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionStmt expressionStmt = new ExpressionStmt(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(expressionStmt);
        expressionStmt.setExpression(expression);
        expressionStmt.setControldependency(structure);

        return expressionStmt;
    }

    private ForStmt visit(ForStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ForStmt forStmt = new ForStmt(_fileName, startLine, endLine, node);
        forStmt.setControldependency(structure);

        ExprList initExprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> initializers = new ArrayList<>();
        if (!node.initializers().isEmpty()) {
            for (Object object : node.initializers()) {
                Expr initializer = (Expr) process((ASTNode) object, structure);
                initializer.setParent(initExprList);
                initializers.add(initializer);
            }
        }
        initExprList.setExprs(initializers);
        initExprList.setParent(forStmt);
        forStmt.setInitializer(initExprList);

        if (node.getExpression() != null) {
            Expr condition = (Expr) process(node.getExpression(), structure);
            condition.setParent(forStmt);
            forStmt.setCondition(condition);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> updaters = new ArrayList<>();
        if (!node.updaters().isEmpty()) {
            for (Object object : node.updaters()) {
                Expr update = (Expr) process((ASTNode) object, forStmt.getCondition());
                update.setParent(exprList);
                updaters.add(update);
            }
        }
        exprList.setExprs(updaters);
        exprList.setParent(forStmt);
        forStmt.setUpdaters(exprList);

        Stmt body = wrapBlock(node.getBody(), forStmt.getCondition());
        body.setStmtParent(forStmt);
        forStmt.setBody(body);

        return forStmt;
    }

    private IfStmt visit(IfStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine   = _cunit.getLineNumber(node.getThenStatement().getStartPosition() + node.getThenStatement().getLength());
//        if(node.getElseStatement() != null)
//            endLine = _cunit.getLineNumber(node.getElseStatement().getStartPosition() + node.getElseStatement().getLength());
//        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        IfStmt ifStmt = new IfStmt(_fileName, startLine, endLine, node);
        ifStmt.setControldependency(structure);

        Expr condition = (Expr) process(node.getExpression(), structure);
        condition.setParent(ifStmt);
        ifStmt.setCondition(condition);

        Stmt then = wrapBlock(node.getThenStatement(), condition);
        then.setStmtParent(ifStmt);
        ifStmt.setThen(then);

//        if (node.getElseStatement() != null) {
//            Stmt els = wrapBlock(node.getElseStatement(), condition);
//            els.setStmtParent(ifStmt);
//            ifStmt.setElse(els);
//        }
        if (node.getElseStatement() != null) {
            Statement elsNode = node.getElseStatement();
            startLine = _cunit.getLineNumber(elsNode.getStartPosition());
            endLine   = _cunit.getLineNumber(elsNode.getStartPosition() + elsNode.getLength());
            ElseStmt els = new ElseStmt(_fileName, startLine, endLine, node.getElseStatement());
            Stmt body = wrapBlock(node.getElseStatement(), condition);
            body.setStmtParent(els);
            els.setBody(body);
            els.setStmtParent(ifStmt);
            ifStmt.setElse(els);
        }
        return ifStmt;
    }

    private LabeledStmt visit(LabeledStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LabeledStmt labeledStmt = new LabeledStmt(_fileName, startLine, endLine, node);
        labeledStmt.setControldependency(structure);
        return labeledStmt;
    }

    private ReturnStmt visit(ReturnStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ReturnStmt returnStmt = new ReturnStmt(_fileName, startLine, endLine, node);
        returnStmt.setControldependency(structure);
        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), structure);
            expression.setParent(returnStmt);
            returnStmt.setExpression(expression);
        }

        return returnStmt;
    }

    private SuperConstructorInv visit(SuperConstructorInvocation node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperConstructorInv superConstructorInv = new SuperConstructorInv(_fileName, startLine, endLine, node);
        superConstructorInv.setControldependency(structure);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), structure);
            expression.setParent(superConstructorInv);
            superConstructorInv.setExpression(expression);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object, structure);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(superConstructorInv);
        superConstructorInv.setArguments(exprList);

        return superConstructorInv;
    }

    private SwCase visit(SwitchCase node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwCase swCase = new SwCase(_fileName, startLine, endLine, node);
        swCase.setControldependency(structure);
        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), structure);
            expression.setParent(swCase);
            swCase.setExpression(expression);
        }
        return swCase;
    }

    private SwitchStmt visit(SwitchStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SwitchStmt switchStmt = new SwitchStmt(_fileName, startLine, endLine, node);
        switchStmt.setControldependency(structure);

        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(switchStmt);
        switchStmt.setExpression(expression);

        List<Stmt> statements = new ArrayList<>();
        Node crldp = null;
        for (Object object : node.statements()) {
            Stmt stmt = (Stmt) process((ASTNode) object, crldp);
            stmt.setStmtParent(switchStmt);
            statements.add(stmt);
            if(stmt instanceof SwCase) {
                crldp = stmt;
            }
        }
        switchStmt.setStatements(statements);
        return switchStmt;
    }

    private SynchronizedStmt visit(SynchronizedStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SynchronizedStmt synchronizedStmt = new SynchronizedStmt(_fileName, startLine, endLine, node);
        synchronizedStmt.setControldependency(structure);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), structure);
            expression.setParent(synchronizedStmt);
            synchronizedStmt.setExpression(expression);
        }

        Blk blk = (Blk) process(node.getBody(), structure);
        blk.setStmtParent(synchronizedStmt);
        synchronizedStmt.setBlock(blk);

        return synchronizedStmt;
    }

    private ThrowStmt visit(ThrowStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThrowStmt throwStmt = new ThrowStmt(_fileName, startLine, endLine, node);
        throwStmt.setControldependency(structure);

        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(throwStmt);
        throwStmt.setExpression(expression);

        return throwStmt;
    }

    private TryStmt visit(TryStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TryStmt tryStmt = new TryStmt(_fileName, startLine, endLine, node);
        tryStmt.setControldependency(structure);


        if (node.resources() != null) {
            List<VarDeclarationExpr> resourceList = new ArrayList<>(node.resources().size());
            for (Object object : node.resources()) {
                VariableDeclarationExpression resource = (VariableDeclarationExpression) object;
                VarDeclarationExpr vdExpr = (VarDeclarationExpr) process(resource, structure);
                vdExpr.setParent(tryStmt);
                resourceList.add(vdExpr);
            }
            tryStmt.setResource(resourceList);
        }
        Blk blk = (Blk) process(node.getBody(), structure);
        blk.setStmtParent(tryStmt);
        tryStmt.setBody(blk);

        List<CatClause> catches = new ArrayList<>(node.catchClauses().size());
        for (Object object : node.catchClauses()) {
            CatchClause catchClause = (CatchClause) object;
            CatClause catClause = (CatClause) process(catchClause, structure);
            catClause.setStmtParent(tryStmt);
            catches.add(catClause);
        }
        tryStmt.setCatchClause(catches);

        if (node.getFinally() != null) {
            Blk finallyBlk = (Blk) process(node.getFinally(), structure);
            finallyBlk.setStmtParent(tryStmt);
            tryStmt.setFinallyBlock(finallyBlk);
        }

        return tryStmt;
    }

    private CatClause visit(CatchClause node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CatClause catClause = new CatClause(_fileName, startLine, endLine, node);
        catClause.setControldependency(structure);

        Svd svd = (Svd) process(node.getException(), structure);
        svd.setParent(catClause);
        catClause.setException(svd);
        Blk body = (Blk) process(node.getBody(), svd);
        body.setStmtParent(catClause);
        catClause.setBody(body);
        return catClause;
    }

    private TypeDeclarationStmt visit(TypeDeclarationStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeDeclarationStmt typeDeclarationStmt = new TypeDeclarationStmt(_fileName, startLine, endLine, node);
        typeDeclarationStmt.setControldependency(structure);
        return typeDeclarationStmt;
    }

    private VarDeclarationStmt visit(VariableDeclarationStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationStmt varDeclarationStmt = new VarDeclarationStmt(_fileName, startLine, endLine, node);
        varDeclarationStmt.setControldependency(structure);
        String modifier = "";
        if (node.modifiers() != null && node.modifiers().size() > 0) {
            for (Object object : node.modifiers()) {
                modifier += " " + object.toString();
            }
        }
        if (modifier.length() > 0) {
            varDeclarationStmt.setModifier(modifier);
        }

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(varDeclarationStmt);
        varDeclarationStmt.setDeclType(mType);

        List<Vdf> fragments = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object, structure);
            vdf.setParent(varDeclarationStmt);
            vdf.setType(mType);
            fragments.add(vdf);
        }
        varDeclarationStmt.setFragments(fragments);

        return varDeclarationStmt;
    }

    private WhileStmt visit(WhileStatement node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        WhileStmt whileStmt = new WhileStmt(_fileName, startLine, endLine, node);
        whileStmt.setControldependency(structure);


        Expr expression = (Expr) process(node.getExpression(), structure);
        expression.setParent(whileStmt);
        whileStmt.setExpression(expression);

        Stmt body = wrapBlock(node.getBody(), expression);
        body.setStmtParent(whileStmt);
        whileStmt.setBody(body);

        return whileStmt;
    }

    /*begin expression*/
    private Comment visit(Annotation node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Comment comment = new Comment(_fileName, startLine, endLine, node);
        return comment;
    }

    private AryAcc visit(ArrayAccess node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryAcc aryAcc = new AryAcc(_fileName, startLine, endLine, node);

        Expr array = (Expr) process(node.getArray(), strcture);
        array.setParent(aryAcc);
        aryAcc.setArray(array);

        Expr indexExpr = (Expr) process(node.getIndex(), strcture);
        indexExpr.setParent(aryAcc);
        aryAcc.setIndex(indexExpr);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        aryAcc.setType(type);

        return aryAcc;
    }

    private AryCreation visit(ArrayCreation node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryCreation aryCreation = new AryCreation(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType().getElementType());
        Type type = typeFromBinding(node.getAST(), node.getType().getElementType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType().getElementType();
        }
        mType.setType(type);
        mType.setParent(aryCreation);
        aryCreation.setArrayType(mType);
        aryCreation.setType(node.getType());

        List<Expr> dimension = new ArrayList<>();
        for (Object object : node.dimensions()) {
            Expr dim = (Expr) process((ASTNode) object, strcture);
            dim.setParent(aryCreation);
            dimension.add(dim);
        }
        aryCreation.setDimension(dimension);

        if (node.getInitializer() != null) {
            AryInitializer arrayInitializer = (AryInitializer) process(node.getInitializer(), strcture);
            arrayInitializer.setParent(aryCreation);
            aryCreation.setInitializer(arrayInitializer);
        }

        return aryCreation;
    }

    private AryInitializer visit(ArrayInitializer node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AryInitializer aryInitializer = new AryInitializer(_fileName, startLine, endLine, node);
        List<Expr> expressions = new ArrayList<>();
        for (Object object : node.expressions()) {
            Expr expr = (Expr) process((ASTNode) object, strcture);
            expr.setParent(aryInitializer);
            expressions.add(expr);
        }
        aryInitializer.setExpressions(expressions);

        return aryInitializer;
    }

    private Assign visit(Assignment node, Node structure) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Assign assign = new Assign(_fileName, startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftHandSide(), structure);
        lhs.setParent(assign);
        lhs.setDataDependency(null);
        assign.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightHandSide(), structure);
        rhs.setParent(assign);
        assign.setRightHandSide(rhs);

        AssignOperator assignOperator = new AssignOperator(_fileName, startLine, endLine, null);
        assignOperator.setOperator(node.getOperator());
        assignOperator.setParent(assign);
        assign.setOperator(assignOperator);
        return assign;
    }

    private BoolLiteral visit(BooleanLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        BoolLiteral literal = new BoolLiteral(_fileName, startLine, endLine, node);
        literal.setValue(node.booleanValue());
        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        literal.setType(type);

        return literal;
    }

    private CastExpr visit(CastExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CastExpr castExpr = new CastExpr(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(castExpr);
        castExpr.setCastType(mType);
        Expr expression = (Expr) process(node.getExpression(), strcture);
        expression.setParent(castExpr);
        castExpr.setExpression(expression);
        castExpr.setType(node.getType());

        return castExpr;
    }

    private CharLiteral visit(CharacterLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CharLiteral charLiteral = new CharLiteral(_fileName, startLine, endLine, node);

        charLiteral.setValue(node);
//        charLiteral.setValue(node.charValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newPrimitiveType(PrimitiveType.CHAR);
        charLiteral.setType(type);

        return charLiteral;
    }

    private ClassInstCreation visit(ClassInstanceCreation node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ClassInstCreation classInstCreation = new ClassInstCreation(_fileName, startLine, endLine, node);

        if (node.getExpression() != null) {
            Expr expression = (Expr) process(node.getExpression(), strcture);
            expression.setParent(classInstCreation);
            classInstCreation.setExpression(expression);
        }

        if (node.getAnonymousClassDeclaration() != null) {
            AnonymousClassDecl anonymousClassDecl = (AnonymousClassDecl) process(node.getAnonymousClassDeclaration(),
                    strcture);
            anonymousClassDecl.setParent(classInstCreation);
            classInstCreation.setAnonymousClassDecl(anonymousClassDecl);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr arg = (Expr) process((ASTNode) object, strcture);
            arg.setParent(exprList);
            arguments.add(arg);
        }
        exprList.setExprs(arguments);
        exprList.setParent(classInstCreation);
        classInstCreation.setArguments(exprList);

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(classInstCreation);
        classInstCreation.setClassType(mType);
        classInstCreation.setType(type);

        return classInstCreation;
    }

    private AnonymousClassDecl visit(AnonymousClassDeclaration node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        AnonymousClassDecl anonymousClassDecl = new AnonymousClassDecl(_fileName, startLine, endLine, node);
        return anonymousClassDecl;
    }

    private ConditionalExpr visit(ConditionalExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ConditionalExpr conditionalExpr = new ConditionalExpr(_fileName, startLine, endLine, node);

        Expr condition = (Expr) process(node.getExpression(), strcture);
        condition.setParent(conditionalExpr);
        conditionalExpr.setCondition(condition);

        Expr first = (Expr) process(node.getThenExpression(), strcture);
        first.setParent(conditionalExpr);
        conditionalExpr.setFirst(first);
        first.setControldependency(condition);

        Expr snd = (Expr) process(node.getElseExpression(), strcture);
        snd.setParent(conditionalExpr);
        conditionalExpr.setSecond(snd);
        snd.setControldependency(condition);

        if (first.getType() != null) {
            conditionalExpr.setType(first.getType());
        } else {
            conditionalExpr.setType(snd.getType());
        }

        return conditionalExpr;
    }

    private CreationRef visit(CreationReference node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        CreationRef creationRef = new CreationRef(_fileName, startLine, endLine, node);
        return creationRef;
    }

    private ExpressionMethodRef visit(ExpressionMethodReference node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ExpressionMethodRef expressionMethodRef = new ExpressionMethodRef(_fileName, startLine, endLine, node);
        return expressionMethodRef;
    }

    private FieldAcc visit(FieldAccess node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        FieldAcc fieldAcc = new FieldAcc(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getExpression(), strcture);
        expression.setParent(fieldAcc);
        fieldAcc.setExpression(expression);

        SName identifier = (SName) process(node.getName(), strcture);
        identifier.setParent(fieldAcc);
        fieldAcc.setIdentifier(identifier);
        identifier.setDataDependency(null);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        fieldAcc.setType(type);

        return fieldAcc;
    }

    private InfixExpr visit(InfixExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InfixExpr infixExpr = new InfixExpr(_fileName, startLine, endLine, node);

        Expr lhs = (Expr) process(node.getLeftOperand(), strcture);
        lhs.setParent(infixExpr);
        infixExpr.setLeftHandSide(lhs);

        Expr rhs = (Expr) process(node.getRightOperand(), strcture);
        rhs.setParent(infixExpr);
        infixExpr.setRightHandSide(rhs);

        InfixOperator infixOperator = new InfixOperator(_fileName, startLine, endLine, null);
        infixOperator.setOperator(node.getOperator());
        infixOperator.setParent(infixExpr);
        infixExpr.setOperator(infixOperator);

        infixExpr.setType(NodeUtils.parseExprType(lhs, node.getOperator().toString(), rhs));

        // process special cases
        if (node.hasExtendedOperands()) {
            lhs = infixExpr;
            for (Object o : node.extendedOperands()) {
                rhs = (Expr) process((Expression) o, strcture);
                infixExpr = new InfixExpr(_fileName, startLine, endLine, (ASTNode) o);
                lhs.setParent(infixExpr);
                infixExpr.setLeftHandSide(lhs);

                rhs.setParent(infixExpr);
                infixExpr.setRightHandSide(rhs);

                infixOperator = new InfixOperator(_fileName, startLine, endLine, null);
                infixOperator.setOperator(node.getOperator());
                infixOperator.setParent(infixExpr);
                infixExpr.setOperator(infixOperator);

                infixExpr.setType(NodeUtils.parseExprType(lhs, node.getOperator().toString(), rhs));
                lhs = infixExpr;
            }
        }

        return infixExpr;
    }

    private InstanceofExpr visit(InstanceofExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        InstanceofExpr instanceofExpr = new InstanceofExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getLeftOperand(), strcture);
        expression.setParent(instanceofExpr);
        instanceofExpr.setExpression(expression);

        MType mType = new MType(_fileName, startLine, endLine, node.getRightOperand());
        mType.setType(node.getRightOperand());
        mType.setParent(instanceofExpr);
        instanceofExpr.setInstanceType(mType);

        AST ast = AST.newAST(AST.JLS8);
        Type exprType = ast.newPrimitiveType(PrimitiveType.BOOLEAN);
        instanceofExpr.setType(exprType);

        return instanceofExpr;
    }

    private LambdaExpr visit(LambdaExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        LambdaExpr lambdaExpr = new LambdaExpr(_fileName, startLine, endLine, node);
        return lambdaExpr;
    }

    private MethodInv visit(MethodInvocation node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodInv methodInv = new MethodInv(_fileName, startLine, endLine, node);

        Expr expression = null;
        if (node.getExpression() != null) {
            expression = (Expr) process(node.getExpression(), strcture);
            expression.setParent(methodInv);
            methodInv.setExpression(expression);
        }

        SName sName = new SName(_fileName, startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(methodInv);
        methodInv.setName(sName);

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, strcture);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(methodInv);
        methodInv.setArguments(exprList);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        methodInv.setType(type);

        return methodInv;
    }

    private MethodRef visit(MethodReference node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        MethodRef methodRef = new MethodRef(_fileName, startLine, endLine, node);
        return methodRef;
    }

    private Label visit(Name node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Label expr = null;
        if (node instanceof SimpleName) {
            SName sName = new SName(_fileName, startLine, endLine, node);

            String name = node.getFullyQualifiedName();
            sName.setName(name);

            Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
            sName.setType(type);
            expr = sName;
        } else if (node instanceof QualifiedName) {
            QualifiedName qualifiedName = (QualifiedName) node;
//			System.out.println(qualifiedName.toString());
            QName qName = new QName(_fileName, startLine, endLine, node);
            SName sname = (SName) process(qualifiedName.getName(), strcture);
            sname.setParent(qName);
            Label label = (Label) process(qualifiedName.getQualifier(), strcture);
            label.setParent(qName);
            qName.setName(label, sname);
            qName.setType(sname.getType());

            expr = qName;
        }
        return expr;
    }

    private NillLiteral visit(NullLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        NillLiteral nillLiteral = new NillLiteral(_fileName, startLine, endLine, node);
        return nillLiteral;
    }

    private NumLiteral visit(NumberLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        String token = node.getToken();
        NumLiteral expr = null;
        try {
            Integer value = Integer.parseInt(token);
            IntLiteral literal = new IntLiteral(_fileName, startLine, endLine, node);
            literal.setValue(value);
            AST ast = AST.newAST(AST.JLS8);
            Type type = ast.newPrimitiveType(PrimitiveType.INT);
            literal.setType(type);
            expr = literal;
        } catch (Exception e) {
        }

        if (expr == null) {
            try {
                long value = Long.parseLong(token);
                LongLiteral literal = new LongLiteral(_fileName, startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.LONG);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            try {
                float value = Float.parseFloat(token);
                FloatLiteral literal = new FloatLiteral(_fileName, startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.FLOAT);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            try {
                double value = Double.parseDouble(token);
                DoubleLiteral literal = new DoubleLiteral(_fileName, startLine, endLine, node);
                literal.setValue(value);
                AST ast = AST.newAST(AST.JLS8);
                Type type = ast.newPrimitiveType(PrimitiveType.DOUBLE);
                literal.setType(type);
                expr = literal;
            } catch (Exception e) {
            }
        }

        if (expr == null) {
            // should be hexadecimal number or octal number
            token = token.replace("X", "x");
            token = token.replace("F", "f");
            NumLiteral literal = new NumLiteral(_fileName, startLine, endLine, node);
            literal.setValue(token);
            // simply set as int type
            AST ast = AST.newAST(AST.JLS8);
            Type type = ast.newPrimitiveType(PrimitiveType.INT);
            literal.setType(type);
            expr = literal;
        }

        return expr;
    }

    private ParenthesiszedExpr visit(ParenthesizedExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());

        ParenthesiszedExpr parenthesiszedExpr = new ParenthesiszedExpr(_fileName, startLine, endLine, node);
        Expr expression = (Expr) process(node.getExpression(), strcture);
        expression.setParent(parenthesiszedExpr);
        parenthesiszedExpr.setExpr(expression);
        parenthesiszedExpr.setType(expression.getType());

        return parenthesiszedExpr;
    }

    private PostfixExpr visit(PostfixExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PostfixExpr postfixExpr = new PostfixExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand(), strcture);
        expression.setParent(postfixExpr);
        postfixExpr.setExpression(expression);

        PostOperator postOperator = new PostOperator(_fileName, startLine, endLine, null);
        postOperator.setOperator(node.getOperator());
        postOperator.setParent(postfixExpr);
        postfixExpr.setOperator(postOperator);

        Type exprType = NodeUtils.parseExprType(expression, node.getOperator().toString(), null);

        postfixExpr.setType(exprType);

//        switch (postOperator.toSrcString().toString()) {
//            case "++":
//            case "--":
//                scope.addDefine(expression.toSrcString().toString(), postfixExpr);
//        }

        return postfixExpr;
    }

    private PrefixExpr visit(PrefixExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        PrefixExpr prefixExpr = new PrefixExpr(_fileName, startLine, endLine, node);

        Expr expression = (Expr) process(node.getOperand(), strcture);
        expression.setParent(prefixExpr);
        prefixExpr.setExpression(expression);

        PrefixOperator prefixOperator = new PrefixOperator(_fileName, startLine, endLine, null);
        prefixOperator.setOperator(node.getOperator());
        prefixOperator.setParent(prefixExpr);
        prefixExpr.setOperator(prefixOperator);

        Type type = NodeUtils.parseExprType(null, node.getOperator().toString(), expression);
        prefixExpr.setType(type);

//        switch (prefixExpr.toSrcString().toString()) {
//            case "++":
//            case "--":
//                scope.addDefine(expression.toSrcString().toString(), prefixExpr);
//        }

        return prefixExpr;
    }

    private StrLiteral visit(StringLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        StrLiteral literal = new StrLiteral(_fileName, startLine, endLine, node);

        literal.setValue(node);
//        literal.setValue(node.getLiteralValue());

        AST ast = AST.newAST(AST.JLS8);
        Type type = ast.newSimpleType(ast.newSimpleName("String"));
        literal.setType(type);

        return literal;
    }

    private SuperFieldAcc visit(SuperFieldAccess node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperFieldAcc superFieldAcc = new SuperFieldAcc(_fileName, startLine, endLine, node);

        SName identifier = (SName) process(node.getName(), strcture);
        identifier.setParent(superFieldAcc);
        superFieldAcc.setIdentifier(identifier);
        identifier.setDataDependency(null);

        if (node.getQualifier() != null) {
            Label name = (Label) process(node.getQualifier(), strcture);
            name.setParent(superFieldAcc);
            superFieldAcc.setName(name);
            name.setDataDependency(null);
        }

        Type exprType = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        superFieldAcc.setType(exprType);
//        superFieldAcc.setDataDependency(scope.getDefines(superFieldAcc.toSrcString().toString()));

        return superFieldAcc;
    }

    private SuperMethodInv visit(SuperMethodInvocation node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodInv superMethodInv = new SuperMethodInv(_fileName, startLine, endLine, node);

        SName sName = new SName(_fileName, startLine, endLine, node.getName());
        sName.setName(node.getName().getFullyQualifiedName());
        sName.setParent(superMethodInv);
        superMethodInv.setName(sName);
        sName.setDataDependency(null);

        if (node.getQualifier() != null) {
            Label label = (Label) process(node.getQualifier(), strcture);
            label.setParent(superMethodInv);
            superMethodInv.setLabel(label);
        }

        ExprList exprList = new ExprList(_fileName, startLine, endLine, null);
        List<Expr> arguments = new ArrayList<>();
        for (Object object : node.arguments()) {
            Expr expr = (Expr) process((ASTNode) object, strcture);
            expr.setParent(exprList);
            arguments.add(expr);
        }
        exprList.setExprs(arguments);
        exprList.setParent(superMethodInv);
        superMethodInv.setArguments(exprList);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        superMethodInv.setType(type);

        return superMethodInv;
    }

    private SuperMethodRef visit(SuperMethodReference node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        SuperMethodRef superMethodRef = new SuperMethodRef(_fileName, startLine, endLine, node);

        return superMethodRef;
    }

    private ThisExpr visit(ThisExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        ThisExpr thisExpr = new ThisExpr(_fileName, startLine, endLine, node);

        Type type = typeFromBinding(node.getAST(), node.resolveTypeBinding());
        thisExpr.setType(type);

        return thisExpr;
    }

    private TyLiteral visit(TypeLiteral node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TyLiteral tyLiteral = new TyLiteral(_fileName, startLine, endLine, node);
        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(tyLiteral);
        tyLiteral.setValue(mType);
        tyLiteral.setType(type);

        return tyLiteral;
    }

    private TypeMethodRef visit(TypeMethodReference node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        TypeMethodRef typeMethodRef = new TypeMethodRef(_fileName, startLine, endLine, node);
        return typeMethodRef;
    }

    private VarDeclarationExpr visit(VariableDeclarationExpression node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        VarDeclarationExpr varDeclarationExpr = new VarDeclarationExpr(_fileName, startLine, endLine, node);

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if(type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(varDeclarationExpr);
        varDeclarationExpr.setDeclType(mType);
        varDeclarationExpr.setType(node.getType());

        List<Vdf> vdfs = new ArrayList<>();
        for (Object object : node.fragments()) {
            Vdf vdf = (Vdf) process((ASTNode) object, strcture);
            vdf.setType(mType);
            vdf.setParent(varDeclarationExpr);
            vdfs.add(vdf);
        }
        varDeclarationExpr.setVarDeclFrags(vdfs);

        return varDeclarationExpr;
    }

    private Vdf visit(VariableDeclarationFragment node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Vdf vdf = new Vdf(_fileName, startLine, endLine, node);

        SName identifier = (SName) process(node.getName(), strcture);
        identifier.setParent(vdf);
        vdf.setName(identifier);
        identifier.setDataDependency(null);

        vdf.setDimensions(node.getExtraDimensions());

        if (node.getInitializer() != null) {
            Expr expression = (Expr) process(node.getInitializer(), strcture);
            expression.setParent(vdf);
            vdf.setExpression(expression);
        }
//        scope.addDefine(identifier.getName(), vdf);

        return vdf;
    }

    private Svd visit(SingleVariableDeclaration node, Node strcture) {
        int startLine = _cunit.getLineNumber(node.getStartPosition());
        int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
        Svd svd = new Svd(_fileName, startLine, endLine, node);
        svd.setVariant(node.isVarargs());

        MType mType = new MType(_fileName, startLine, endLine, node.getType());
        Type type = typeFromBinding(node.getAST(), node.getType().resolveBinding());
        if (type == null || type instanceof WildcardType) {
            type = node.getType();
        }
        mType.setType(type);
        mType.setParent(svd);
        svd.setDecType(mType);
        svd.setType(node.getType());
        if (node.getInitializer() != null) {
            Expr initializer = (Expr) process(node.getInitializer(), strcture);
            initializer.setParent(svd);
            svd.setInitializer(initializer);
        }

        SName name = (SName) process(node.getName(), strcture);
        name.setParent(svd);
        svd.setName(name);
        name.setDataDependency(null);

//        scope.addDefine(name.getName(), svd);
        return svd;
    }

    public Node process(ASTNode node) {
        return process(node,  null);
    }

    public Node process(ASTNode node,  Node structure) {
        if (node == null) {
            return null;
        }
        if (node instanceof AssertStatement) {
            return visit((AssertStatement) node, structure);
        } else if (node instanceof Block) {
            return visit((Block) node, structure);
        } else if (node instanceof BreakStatement) {
            return visit((BreakStatement) node, structure);
        } else if (node instanceof ConstructorInvocation) {
            return visit((ConstructorInvocation) node, structure);
        } else if (node instanceof ContinueStatement) {
            return visit((ContinueStatement) node, structure);
        } else if (node instanceof DoStatement) {
            return visit((DoStatement) node, structure);
        } else if (node instanceof EmptyStatement) {
            return visit((EmptyStatement) node, structure);
        } else if (node instanceof EnhancedForStatement) {
            return visit((EnhancedForStatement) node, structure);
        } else if (node instanceof ExpressionStatement) {
            return visit((ExpressionStatement) node, structure);
        } else if (node instanceof ForStatement) {
            return visit((ForStatement) node, structure);
        } else if (node instanceof IfStatement) {
            return visit((IfStatement) node, structure);
        } else if (node instanceof LabeledStatement) {
            return visit((LabeledStatement) node, structure);
        } else if (node instanceof ReturnStatement) {
            return visit((ReturnStatement) node, structure);
        } else if (node instanceof SuperConstructorInvocation) {
            return visit((SuperConstructorInvocation) node, structure);
        } else if (node instanceof SwitchCase) {
            return visit((SwitchCase) node, structure);
        } else if (node instanceof SwitchStatement) {
            return visit((SwitchStatement) node, structure);
        } else if (node instanceof SynchronizedStatement) {
            return visit((SynchronizedStatement) node, structure);
        } else if (node instanceof ThrowStatement) {
            return visit((ThrowStatement) node, structure);
        } else if (node instanceof TryStatement) {
            return visit((TryStatement) node, structure);
        } else if (node instanceof TypeDeclarationStatement) {
            return visit((TypeDeclarationStatement) node, structure);
        } else if (node instanceof VariableDeclarationStatement) {
            return visit((VariableDeclarationStatement) node, structure);
        } else if (node instanceof WhileStatement) {
            return visit((WhileStatement) node, structure);
        } else if (node instanceof Annotation) {
            return visit((Annotation) node, structure);
        } else if (node instanceof ArrayAccess) {
            return visit((ArrayAccess) node, structure);
        } else if (node instanceof ArrayCreation) {
            return visit((ArrayCreation) node, structure);
        } else if (node instanceof ArrayInitializer) {
            return visit((ArrayInitializer) node, structure);
        } else if (node instanceof Assignment) {
            return visit((Assignment) node, structure);
        } else if (node instanceof BooleanLiteral) {
            return visit((BooleanLiteral) node, structure);
        } else if (node instanceof CastExpression) {
            return visit((CastExpression) node, structure);
        } else if (node instanceof CharacterLiteral) {
            return visit((CharacterLiteral) node, structure);
        } else if (node instanceof ClassInstanceCreation) {
            return visit((ClassInstanceCreation) node, structure);
        } else if (node instanceof ConditionalExpression) {
            return visit((ConditionalExpression) node, structure);
        } else if (node instanceof CreationReference) {
            return visit((CreationReference) node, structure);
        } else if (node instanceof ExpressionMethodReference) {
            return visit((ExpressionMethodReference) node, structure);
        } else if (node instanceof FieldAccess) {
            return visit((FieldAccess) node, structure);
        } else if (node instanceof InfixExpression) {
            return visit((InfixExpression) node, structure);
        } else if (node instanceof InstanceofExpression) {
            return visit((InstanceofExpression) node, structure);
        } else if (node instanceof LambdaExpression) {
            return visit((LambdaExpression) node, structure);
        } else if (node instanceof MethodInvocation) {
            return visit((MethodInvocation) node, structure);
        } else if (node instanceof MethodReference) {
            return visit((MethodReference) node, structure);
        } else if (node instanceof Name) {
            return visit((Name) node, structure);
        } else if (node instanceof NullLiteral) {
            return visit((NullLiteral) node, structure);
        } else if (node instanceof NumberLiteral) {
            return visit((NumberLiteral) node, structure);
        } else if (node instanceof ParenthesizedExpression) {
            return visit((ParenthesizedExpression) node, structure);
        } else if (node instanceof PostfixExpression) {
            return visit((PostfixExpression) node, structure);
        } else if (node instanceof PrefixExpression) {
            return visit((PrefixExpression) node, structure);
        } else if (node instanceof StringLiteral) {
            return visit((StringLiteral) node, structure);
        } else if (node instanceof SuperFieldAccess) {
            return visit((SuperFieldAccess) node, structure);
        } else if (node instanceof SuperMethodInvocation) {
            return visit((SuperMethodInvocation) node, structure);
        } else if (node instanceof SuperMethodReference) {
            return visit((SuperMethodReference) node, structure);
        } else if (node instanceof ThisExpression) {
            return visit((ThisExpression) node, structure);
        } else if (node instanceof TypeLiteral) {
            return visit((TypeLiteral) node, structure);
        } else if (node instanceof TypeMethodReference) {
            return visit((TypeMethodReference) node, structure);
        } else if (node instanceof VariableDeclarationExpression) {
            return visit((VariableDeclarationExpression) node, structure);
        } else if (node instanceof AnonymousClassDeclaration) {
            return visit((AnonymousClassDeclaration) node, structure);
        } else if (node instanceof VariableDeclarationFragment) {
            return visit((VariableDeclarationFragment) node, structure);
        } else if (node instanceof SingleVariableDeclaration) {
            return visit((SingleVariableDeclaration) node, structure);
        } else if (node instanceof MethodDeclaration) {
            return visit((MethodDeclaration) node, structure);
        } else if (node instanceof CatchClause) {
            return visit((CatchClause) node, structure);
        } else {
            LevelLogger.error("UNKNOWN ASTNode type : " + node.toString());
            return null;
        }
    }

    private static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
        if (typeBinding == null) {
            return ast.newWildcardType();
        }

        if (typeBinding.isPrimitive()) {
            return ast.newPrimitiveType(
                    PrimitiveType.toCode(typeBinding.getName()));
        }

        if (typeBinding.isCapture()) {
            ITypeBinding wildCard = typeBinding.getWildcard();
            WildcardType capType = ast.newWildcardType();
            ITypeBinding bound = wildCard.getBound();
            if (bound != null) {
                capType.setBound(typeFromBinding(ast, bound),
                        wildCard.isUpperbound());
            }
            return capType;
        }

        if (typeBinding.isArray()) {
            Type elType = typeFromBinding(ast, typeBinding.getElementType());
            return ast.newArrayType(elType, typeBinding.getDimensions());
        }

        if (typeBinding.isParameterizedType()) {
            ParameterizedType type = ast.newParameterizedType(
                    typeFromBinding(ast, typeBinding.getErasure()));

            @SuppressWarnings("unchecked")
            List<Type> newTypeArgs = type.typeArguments();
            for (ITypeBinding typeArg : typeBinding.getTypeArguments()) {
                newTypeArgs.add(typeFromBinding(ast, typeArg));
            }

            return type;
        }

        if (typeBinding.isWildcardType()) {
            WildcardType type = ast.newWildcardType();
            if (typeBinding.getBound() != null) {
                type.setBound(typeFromBinding(ast, typeBinding.getBound()));
            }
            return type;
        }

//        if(typeBinding.isGenericType()) {
//            System.out.println(typeBinding.toString());
//            return typeFromBinding(ast, typeBinding.getErasure());
//        }

        // simple or raw type
        String qualName = typeBinding.getName();
        if ("".equals(qualName)) {
            return ast.newWildcardType();
        }
        try {
            return ast.newSimpleType(ast.newName(qualName));
        } catch (Exception e) {
            return ast.newWildcardType();
        }
    }

    private Blk wrapBlock(Statement node, Node strcture) {
        Blk blk;
        if(node instanceof Block) {
            blk = (Blk) process(node, strcture);
        } else {
            int startLine = _cunit.getLineNumber(node.getStartPosition());
            int endLine = _cunit.getLineNumber(node.getStartPosition() + node.getLength());
            blk = new Blk(_fileName, startLine, endLine, node);
            List<Stmt> stmts = new ArrayList<>();
            Stmt stmt = (Stmt) process(node, strcture);
            stmt.setStmtParent(blk);
            stmts.add(stmt);
            blk.setStatement(stmts);
        }
        return blk;
    }

}
