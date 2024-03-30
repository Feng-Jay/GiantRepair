package mfix.core.stats.element;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.common.util.Pair;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class ElementCounter {
    private DatabaseConnector _connector = null;

    private static Hashtable<Pair<String, Integer>, Integer> cacheMapForAPI = null;
    private static HashMap<Pair<Pair<String, Integer>, String>, Integer> cacheMapForAPIWithType = null;

    private static Integer cacheTotalNumberforAPI = null;
    private static Integer cacheTotalNumberforAPIWithType = null;

    static final String WARNNING_FOR_QUERY_DB = "[WARNING] Query on Database.";

    public void open() {
        _connector = new DatabaseConnector();
        _connector.open();
    }

    public void close() {
        _connector.close();
    }

    public void add(Element element) throws ElementException {
        _connector.add(element.toInsertRow());
    }

    public float count(Element element, ElementQueryType queryType) throws ElementException {
        Integer countNumber, allNumber = 0;
        if ((cacheMapForAPI != null) && (element instanceof MethodElement) && (!queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            MethodElement methodElement = (MethodElement)element;

            if (methodElement._elementName == null) {
                throw new ElementException(element.DBKEY_ELEMENT_NAME);
            }
            if (methodElement._argsNumber == null) {
                throw new ElementException(element.DBKEY_ARGS_NUMBER);
            }
            countNumber = cacheMapForAPI.getOrDefault(new Pair<String, Integer>(methodElement._elementName, methodElement._argsNumber), 0);

            if (queryType.getWithPercent()) {
                if (cacheTotalNumberforAPI == null) {
                    LevelLogger.warn(WARNNING_FOR_QUERY_DB);
                    cacheTotalNumberforAPI = _connector.query(element.toQueryRowWithoutLimit(queryType));
                }
                allNumber = cacheTotalNumberforAPI;
            }
        } else if ((cacheMapForAPIWithType != null) && (element instanceof MethodElement) && (queryType.getWithType()) && (queryType._countType == ElementQueryType.CountType.COUNT_FILES)) {
            MethodElement methodElement = (MethodElement)element;

            if (methodElement._elementName == null) {
                throw new ElementException(element.DBKEY_ELEMENT_NAME);
            }
            if (methodElement._argsNumber == null) {
                throw new ElementException(element.DBKEY_ARGS_NUMBER);
            }
            if (methodElement._objType == null) {
                throw new ElementException(element.DBKEY_OBJ_TYPE);
            }
            countNumber = cacheMapForAPIWithType.getOrDefault(new Pair<>(new Pair<>(methodElement._elementName, methodElement._argsNumber), methodElement._objType), 0);

            if (queryType.getWithPercent()) {
                if (cacheTotalNumberforAPIWithType == null) {
                    LevelLogger.warn(WARNNING_FOR_QUERY_DB);
                    cacheTotalNumberforAPIWithType = _connector.query(element.toQueryRowWithoutLimit(queryType));
                }
                allNumber = cacheTotalNumberforAPIWithType;
            }
        } else {
            LevelLogger.warn(WARNNING_FOR_QUERY_DB);
            countNumber = _connector.query(element.toQueryRow(queryType));
            allNumber = _connector.query(element.toQueryRowWithoutLimit(queryType));
        }

        if (queryType.getWithPercent()) {
            return allNumber == 0 ? 0 : ((float)countNumber) / allNumber;
        } else {
            return countNumber;
        }
    }

    public void loadCacheWithoutType(String cacheFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(cacheFile));
        String line;
        cacheMapForAPI = new Hashtable<>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String elementName = st.nextToken();
            Integer argsNumber = Integer.parseInt(st.nextToken());
            Integer countNumber = Integer.parseInt(st.nextToken());
            cacheMapForAPI.put(new Pair<>(elementName, argsNumber), countNumber);
        }
    }

    public void loadCacheWithType(String cacheFile) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(cacheFile));
        String line;
        cacheMapForAPIWithType = new HashMap<>();
        while ((line = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(line);
            String elementName = st.nextToken();
            Integer argsNumber = Integer.parseInt(st.nextToken());
            String objType = st.nextToken();
            Integer countNumber = Integer.parseInt(st.nextToken());
            cacheMapForAPIWithType.put(new Pair<>(new Pair<>(elementName, argsNumber), objType), countNumber);
        }
    }

}
