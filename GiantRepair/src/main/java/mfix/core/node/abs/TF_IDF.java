package mfix.core.node.abs;

import mfix.common.conf.Constant;
import mfix.common.util.JavaFile;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.Node;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TypeLiteral;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;
public class TF_IDF implements CodeAbstraction {

    private static Map<String, Integer> _tokenMap;
    private final static int TOTAL_FILE_NUM = Constant.TOTAL_BUGGY_FILE_NUMBER;

    private double _threshold;
    private String _fileName;
    private Map<String, Integer> _tokenMapInFile;
    private double _tokenSizeInFile = 0;

    static {
        try {
            loadTokenMap(Constant.TF_IDF_TOKENS);
        } catch (IOException e) {
            LevelLogger.fatal("Load token mapping ");
        }
    }

    public TF_IDF(String fileName, double threshold) {
        _fileName = fileName;
        _threshold = threshold;
    }

    @Override
    public TF_IDF lazyInit() {
        if (_tokenMapInFile == null) {
            _tokenMapInFile = new Hashtable<>();
            TokenCollector tokenCollector = new TokenCollector(_fileName);
            _tokenSizeInFile = tokenCollector.collect().tokenSize();
        }
        return this;
    }

    private static void loadTokenMap(String mapFile) throws IOException {
        File file = new File(mapFile);
        if (!file.exists()) {
            throw new IOException("Token mapping file does not exist : " + file.getAbsolutePath());
        }
        _tokenMap = new Hashtable<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String token = br.readLine();
        String number = br.readLine();
        Integer num;
        while(token != null && number != null) {
            try {
                num = Integer.parseInt(number);
                _tokenMap.put(token, num);
            } catch (Exception e) {
            }
            token = br.readLine();
            number = br.readLine();
        }
        br.close();
    }

    @Override
    public boolean shouldAbstract(Node node) {
        String token = node.toSrcString().toString();
        double numInFile = _tokenMapInFile.getOrDefault(token, 1);
        double numInDoc = _tokenMap.getOrDefault(token, 1) + 1;
        double tf = numInFile / _tokenSizeInFile;
        double idf = Math.log10(TOTAL_FILE_NUM / numInDoc);
        // The smaller the value of TF_IDF, the more prevalent of the token
        double tfidf = tf * idf;
        return tfidf > _threshold;
    }

    @Override
    public boolean shouldAbstract(String string) {
        return false;
    }

    @Override
    public boolean shouldAbstract(Node node, Category category) {
        return false;
    }

    @Override
    public boolean shouldAbstract(String string, Category category) {
        return false;
    }

    private class TokenCollector extends ASTVisitor {

        private String _file;
        private int _size = 0;

        public TokenCollector(String file) {
            _file = file;
        }

        public TokenCollector collect() {
            CompilationUnit unit = JavaFile.genASTFromFileWithType(_file);
            unit.accept(this);
            return this;
        }

        public int tokenSize() {
            return _size;
        }

        private void addToken(String token) {
            Integer num = _tokenMapInFile.get(token);
            if (num == null) {
                num = 0;
            }
            num ++;
            _size ++;
            _tokenMapInFile.put(token, num);
        }

        public boolean visit(SimpleName name) {
            addToken(name.getFullyQualifiedName());
            return true;
        }

        public boolean visit(NumberLiteral name) {
            addToken(name.getToken());
            return true;
        }

        public boolean visit(StringLiteral name) {
            addToken(name.getLiteralValue());
            return true;
        }

        public boolean visit(CharacterLiteral literal) {
            addToken(literal.getEscapedValue());
            return true;
        }

        public boolean visit(TypeLiteral literal) {
            addToken(literal.toString());
            return true;
        }

        public boolean visit(BooleanLiteral literal) {
            addToken(literal.toString());
            return true;
        }

        public boolean visit(NullLiteral literal) {
            addToken("null");
            return true;
        }

    }
}
