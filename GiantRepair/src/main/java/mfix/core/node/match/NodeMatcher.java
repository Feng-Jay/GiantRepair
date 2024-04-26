package mfix.core.node.match;

import mfix.common.conf.Constant;
import mfix.common.util.LevelLogger;
import mfix.core.node.ast.MethDecl;
import mfix.core.node.ast.Node;
import mfix.core.node.ast.stmt.Stmt;
import mfix.core.node.check.StaticCheck;
import mfix.core.node.match.metric.LevenShteinDistance;
import mfix.core.node.modify.Generator;
import mfix.core.node.modify.MyActions;
import mfix.tools.Timer;

import java.util.*;
import java.util.concurrent.Callable;

public class NodeMatcher implements Callable<List<List<List<MyActions>>>> {

    private final Node _bNode;
    private final Node _pNode;
    private List<Integer> _buggyLines;
    private MatchLevel _level;
    private final Timer _timer;

    private final List<Integer> _buggyChanges;
    private final List<Integer> _patchChanges;

    private final List<Integer> _addBrackets;

    private final List<Integer> _deleteBrackets;
    private final Map<Integer, Integer> _keepMap;

    private final Integer MAXDIFFCHILDREN = 2;

    private final StaticCheck _checker;

    private final String _logfile;

    public NodeMatcher(Node bNode, Node pNode, List<Integer> buggyChanges, List<Integer> patchChanges,
                       List<Integer> addBracket, List<Integer> deleteBracket, Map<Integer, Integer> keepMap, int minutes, StaticCheck checker, String logfile) {
        _bNode = bNode;
        _pNode = pNode;
        _buggyChanges = buggyChanges;
        _patchChanges = patchChanges;
        _addBrackets  = addBracket;
        _deleteBrackets = deleteBracket;
        _keepMap = keepMap;
        _timer = new Timer(minutes);
        _timer.start();
        _checker = checker;
        _logfile = logfile;
    }


    public MatchLevel getMatchLevel() {
        return _level;
    }

    @Override
    public List<List<List<MyActions>>> call() {
        _timer.start();
        List<List<List<MyActions>>> fixPositions;
        fixPositions = tryMatch(_bNode, _pNode, _buggyLines);
        return fixPositions;
    }

    /**
     * Try to figure out all possible matching solutions between
     * buggy node {@code buggy} and _pattern node {@code _pattern}.
     *
     * @param buggy   : buggy node
     * @param patch : _pattern node
     * @return : a set of possible solutions
     */
//    public List<List<List<MyActions>>> tryMatch(Node buggy, Node patch) {
//        return tryMatch(buggy, patch, null);
//    }
//
//    public List<List<List<MyActions>>> tryMatch(Node buggy, Node patch, List<Integer> buggyLines) {
//        return tryMatch(buggy, patch, buggyLines, Constant.MAX_INSTANCE_PER_PATTERN, MatchLevel.ALL);
//    }
//
//    public List<List<List<MyActions>>> tryMatch(Node buggy, Node patch, List<Integer> buggyLines,
//                                        MatchLevel level) {
//        return tryMatch(buggy, patch, buggyLines, Constant.MAX_INSTANCE_PER_PATTERN, level);
//    }


    /**
     * Use Set to de-duplicate same items in List
     * @param stmts: statement need to de-duplicate
     * @return: return a de-duplicated list
     */
    public List<Node> deDuplicated(List<Node> stmts){
        Set<Node> tmpSet = new HashSet<>(stmts);
        List<Node> ret   = new ArrayList<>(tmpSet);
        for(Node node: ret){
            if(node instanceof MethDecl && (node.toString().equals(_bNode.toString()) || node.toString().equals(_pNode.toString()))){
                ret.remove(node);
                break;
            }
        }
        ret.sort(Comparator.comparing(Node::getStartLine));
        return ret;
    }

    /**
     * Get the modified statements in the patch file
     * @param stmts: all statements in the patch file
     * @return: return a list of modified statements
     */
    public List<Node> filterPatch(List<Node> stmts){
        List<Node> minimumNodes = new ArrayList<>();
        for(int line: _patchChanges){
            Node tmp = null;
            int minimum = Integer.MAX_VALUE;
            for(Node node: stmts){
                if(node.getStartLine() <= line && node.getEndLine()>=line && (node.getEndLine()-node.getStartLine()+1)<minimum){
                    tmp = node;
                    minimum =node.getEndLine()-node.getStartLine()+1;
                }
            }
            if(tmp == null){
                System.err.println("No changed stmts find!!!"+" Patch:"+line);
                continue;
            }
            if(minimumNodes.contains(tmp)){
                continue;
            }
            minimumNodes.add(tmp);
        }
        for(int i=0; i< minimumNodes.size(); ++i){
            Node mini = minimumNodes.get(i);
            while(mini!= null && mini.getStmtParent()!=null && mini.getStmtParent().getStartLine()== mini.getStartLine() && mini.getStmtParent().getEndLine() == mini.getEndLine()){
                minimumNodes.set(i, mini.getStmtParent());
                mini = mini.getStmtParent();
            }
        }
        minimumNodes = deDuplicated(minimumNodes);
        return minimumNodes;
    }

    /**
     * Get the modified statements in the buggy file
     * @param stmts: statements in the buggy file
     * @return: return a list of modified statements
     */
    public List<Node> filterBuggy(List<Node> stmts){
        List<Node> minimumNodes = new ArrayList<>();
        for(int line: _buggyChanges){
            Node tmp = null;
            int minimum = Integer.MAX_VALUE;
            for(Node node: stmts){
                if(node.getStartLine() <= line && node.getEndLine()>=line && (node.getEndLine()-node.getStartLine()+1)<minimum){
                    tmp = node;
                    minimum =node.getEndLine()-node.getStartLine()+1;
                }
            }
            if(tmp == null){
                System.err.println("No changed stmts find!!!"+" Buggy:"+line);
                continue;
            }
            if(minimumNodes.contains(tmp)){
                continue;
            }
            minimumNodes.add(tmp);
        }
        for(int i=0; i< minimumNodes.size(); ++i){
            Node mini = minimumNodes.get(i);
            while (mini != null && mini.getStmtParent() != null && mini.getStmtParent().getStartLine()== mini.getStartLine() && mini.getStmtParent().getEndLine() == mini.getEndLine()){
                minimumNodes.set(i, mini.getStmtParent());
                mini = mini.getStmtParent();
            }
        }
        minimumNodes = deDuplicated(minimumNodes);

        return minimumNodes;
    }

    /**
     * Filter out all statements in a list of nodes
     * @param nodes: a list of nodes
     * @return: return a list of statements
     */
    public List<Node> filterStmts(List<Node> nodes){
        List<Node> stmts = new ArrayList<>();
        for(Node item: nodes){
            if(item instanceof Stmt){
                stmts.add(item);
            }
        }
        return stmts;
    }

//    /**
//     * if a node is a block, try to expand it to its parent node
//     * @param nodes
//     * @return
//     */
//    public List<Node> expand(List<Node> nodes){
//       for(int i =0; i < nodes.size(); ++i){
//           Node node = nodes.get(i);
//           if(node instanceof Blk && node.getStmtParent() != null && node.getStmtParent().getStartLine() == node.getStartLine() && node.getStmtParent().getEndLine() == node.getEndLine()){
//               nodes.set(i, node.getStmtParent());
//           }
//       }
//       Set<Node> tmpSet = new HashSet<>(nodes);
//       List<Node> ret = new ArrayList<>(tmpSet);
//       ret.sort(Comparator.comparing(Node::getStartLine));
//       return ret;
//    }

    /**
     * get the statements that are not modified
     * @param allNodes: all statements in the file
     * @param modifiedNodes: modified statements
     * @return: return a list of statements that are not modified
     */
    public List<Node> getKeepNodes(List<Node>allNodes, List<Node> modifiedNodes){
        List<Node> ret = new ArrayList<>();
        for(Node node: allNodes){
            if(!modifiedNodes.contains(node)){
                ret.add(node);
            }
        }
        for(int i=0; i< ret.size(); ++i){
            Node keepNode = ret.get(i);
            while (keepNode != null && keepNode.getStmtParent() != null && keepNode.getStmtParent().getStartLine()== keepNode.getStartLine() && keepNode.getStmtParent().getEndLine() == keepNode.getEndLine()){
                ret.set(i, keepNode.getStmtParent());
                keepNode = keepNode.getStmtParent();
            }
        }
        ret = deDuplicated(ret);
        return ret;
    }

    public Map<Node, Node> mapKeep(List<Node> bKeep, List<Node> pKeep){
//        bKeep.sort(Comparator.comparing(Node::getStartLine).thenComparing(Node::getLength));
//        pKeep.sort(Comparator.comparing(Node::getStartLine).thenComparing(Node::getLength));
        List<Node> alreadyMatches = new ArrayList<>();
        Map<Node, Node> retMap = new HashMap<>();
        for(Node pk: pKeep){
            for(Node bk: bKeep){
                if(alreadyMatches.contains(bk)){
                    continue;
                }
                if(pk.getNodeType().equals(bk.getNodeType())){
                    retMap.put(pk, bk);
                    alreadyMatches.add(bk);
                    break;
                }

            }
        }
        return retMap;
    }

    public List<Node> getLeafNodes(List<Node> oriStmts){
        List<Node> allNodes = new ArrayList<>();
        List<Node> allStmts = new ArrayList<>(oriStmts);
        for(Node first: allStmts){
            boolean match = false;
            for(Node second: allStmts){
                if(second.getStmtParent().equals(first)){
                    match = true;
                    break;
                }
            }
            if(!match) allNodes.add(first);
        }
        return allNodes;
    }

    public List<Node> getNeighbors(List<Node> allStmts, Node currentNode){
        int curIndex = allStmts.indexOf(currentNode);
        // considered neighbors' range
        int MAXSPREADNUM =1;
        List<Node> ret = new ArrayList<>(Collections.nCopies(MAXSPREADNUM*2, null));
//        List<Node> ret = new ArrayList<>(MAXSPREADNUM*2);
        // find former nodes

        for(int i=1; i<=MAXSPREADNUM; ++i){
            int formerIndex = curIndex - i;
            if(formerIndex >= 0) ret.set(MAXSPREADNUM - i, allStmts.get(formerIndex));
            else ret.set(MAXSPREADNUM - i, null);

            int afterIndex = curIndex + i;
            if(afterIndex < allStmts.size()) ret.set(MAXSPREADNUM + i - 1, allStmts.get(afterIndex));
            else ret.set(MAXSPREADNUM + i - 1, null);
        }
        return ret;
    }

    public void setLayers(List<Node> allStmts){
        for(Node stmt: allStmts){
            int index = allStmts.indexOf(stmt);
            int layer = 0;
            if(stmt.getStmtParent() == null){
                stmt.setLayer(0);
            }
            while(stmt.getStmtParent() != null){
                layer += 1;
                stmt = stmt.getStmtParent();
            }
            allStmts.get(index).setLayer(layer);
        }
    }

    public String getAncestor(Node node){
        if(node.getStmtParent() == null){
            return "";
        }
        String ret = "";
        Node parent = node.getStmtParent();
        while(parent != null){
            ret = ret + parent.getNodeType()+";";
            parent = parent.getStmtParent();
        }
        return ret;
    }

    public List<List<List<MyActions>>> tryMatch(Node buggy, Node patch, List<Integer> buggyLines) {
        List<Node> bAllStmts = new ArrayList<>(filterStmts(new ArrayList<>(buggy.flattenTreeNode(new ArrayList<>()))));
        List<Node> bLeaves = getLeafNodes(bAllStmts);
        bAllStmts.sort(Comparator.comparing(Node::getStartLine).thenComparing(Node::getChildrenNum));
        setLayers(bAllStmts);
//        bAllStmts = expand(bAllStmts);
        List<Node> bModifiedStmts = new ArrayList<>(filterBuggy(bAllStmts));
        List<Node> bKeep1 =  getKeepNodes(bAllStmts, bModifiedStmts);

        List<Node> pAllStmts = new ArrayList<>(filterStmts(new ArrayList<>(patch.flattenTreeNode(new ArrayList<>()))));
        List<Node> pLeaves = getLeafNodes(pAllStmts);
        pAllStmts.sort(Comparator.comparing(Node::getStartLine).thenComparing(Node::getChildrenNum));
        setLayers(pAllStmts);
//        pAllStmts = expand(pAllStmts);
        List<Node> pModifiedStmts = new ArrayList<>(filterPatch(pAllStmts));
        List<Node> pKeep1 = getKeepNodes(pAllStmts, pModifiedStmts);

        Map<Node, Node> keep1 = mapKeep(bKeep1, pKeep1);

        // *********************** Match Processing...***************************//
        // patch node -> <bug node, similarity>
        Map<Node, Map<Node, Double>> mapRecord = new HashMap<>();

        // parent node -> list of children node: Bottom-Up matching
        Map<Node, List<Node>> bParentChildren = new HashMap<>();
        Map<Node, List<Node>> pParentChildren = new HashMap<>();

        // current nodes' parent list; for now, they are leaf nodes' parent.
        List<Node> bParentList = new ArrayList<>();// space for time...
        List<Node> pParentList = new ArrayList<>();

        // first compute leaf nodes' similarity
        for(Node pl: pLeaves){
            // get the patch leave node's parent.
            // maintain the parent-children relationship & parent list
            if(pl.getStmtParent() != null){
                if(!pParentList.contains(pl.getStmtParent())) {
                    pParentList.add(pl.getStmtParent());
                    List<Node> tmp = new ArrayList<>(); tmp.add(pl);
                    pParentChildren.put(pl.getStmtParent(), tmp);
                }
                else{
                    pParentChildren.get(pl.getStmtParent()).add(pl);
                }
            }
            Map<Node, Double> tmpMap = new HashMap<>();
            for(Node bl: bLeaves){
                double sim = Mapping.computeSim(pl, bl, getNeighbors(pAllStmts, pl), getNeighbors(bAllStmts, bl),
                        pAllStmts.indexOf(pl), bAllStmts.indexOf(bl));
                tmpMap.put(bl,sim);
            }
            mapRecord.put(pl, tmpMap);
        }
        // maintain parent-children relationship & parent list for buggy file
        for(Node bl: bLeaves){
            if(bl.getStmtParent() != null){
                if(!bParentList.contains(bl.getStmtParent())) {
                    bParentList.add(bl.getStmtParent());
                    List<Node> tmp = new ArrayList<>(); tmp.add(bl);
                    bParentChildren.put(bl.getStmtParent(), tmp);
                }
                else{
                    bParentChildren.get(bl.getStmtParent()).add(bl);
                }
            }
        }

        // Leaf nodes mapping Done, Begin recursively matching
        Queue<Node> pQueue = new LinkedList<>();
        pParentList.sort(Comparator.comparing(Node::getLayer).reversed());
        pQueue.addAll(pParentList); pParentList.clear();

        Queue<Node> bQueue = new LinkedList<>();
        bParentList.sort(Comparator.comparing(Node::getLayer).reversed());
        bQueue.addAll(bParentList); bParentList.clear();

        // TODO: use priority queue
        while(!pQueue.isEmpty()){
            Node ptmp = pQueue.poll();
            List<Node> pSons = pParentChildren.get(ptmp);
            Map<Node, Double> tmpMapRecord= new HashMap<>();
            for(Node btmp: bQueue){
                double parentScore = 0.0;
                // if node-type is different, similarity is 0.0
                if(!ptmp.getNodeType().equals(btmp.getNodeType())){
                    tmpMapRecord.put(btmp, parentScore);
                    continue;
                }
                // Bottom-up matching: two nodes' similarity is the average of their children's max similarity
                List<Node> bSons = bParentChildren.get(btmp);
                for(Node pson: pSons){
                   Map<Node, Double> pMatch = mapRecord.get(pson);
                   double maxScoreTmp = 0;
                   for(Node bson: bSons){
                       if(pMatch.containsKey(bson)){
                           if(pMatch.get(bson) >= maxScoreTmp) maxScoreTmp = pMatch.get(bson);
                       }
                   }
                   parentScore += maxScoreTmp;
                }
                parentScore = parentScore / (Math.max(pSons.size(), bSons.size()));
                tmpMapRecord.put(btmp, parentScore);
            }
            mapRecord.put(ptmp, tmpMapRecord);
            // ptmp mapping done
            // add its parent and store parent-children relationship
            if(ptmp.getStmtParent()!=null){
                if(!pParentList.contains(ptmp.getStmtParent())){
                    pParentList.add(ptmp.getStmtParent());
                    List<Node> tmp = new ArrayList<>(); tmp.add(ptmp);
                    pParentChildren.put(ptmp.getStmtParent(), tmp);
                }else{
                    pParentChildren.get(ptmp.getStmtParent()).add(ptmp);
                }
            }
            if(pQueue.isEmpty()){
                pParentList.sort(Comparator.comparing(Node::getLayer).reversed());
                pQueue.addAll(pParentList);
                for(Node btmp: bQueue){
                    if(btmp.getStmtParent() == null) continue;
                    if(!bParentList.contains(btmp.getStmtParent())){
                        bParentList.add(btmp.getStmtParent());
                        List<Node> tmp = new ArrayList<>(); tmp.add(btmp);
                        bParentChildren.put(btmp.getStmtParent(), tmp);
                    }else{
                        bParentChildren.get(btmp.getStmtParent()).add(btmp);
                    }
                }
                bQueue.clear();
                bParentList.sort(Comparator.comparing(Node::getLayer).reversed());
                bQueue.addAll(bParentList);
                pParentList.clear();
                bParentList.clear();
            }
        }

        // matching process is done; getting the node's similarity
        // generating modifications...
        List<Node> pMatched1 =new ArrayList<>();
        List<Node> bMatched1 = new ArrayList<>();
        List<Mapping> allMatchingPairs = new ArrayList<>();
        List<Mapping> filteredMatchingPairs = new ArrayList<>();
        List<Mapping> finalMatchingPairs = new ArrayList<>();

        // Get all matching pairs
        for(Node pModi: pModifiedStmts){
            Map<Node, Double> record = mapRecord.get(pModi);
            for(Node bModi: bModifiedStmts){
                if(record.containsKey(bModi))
                {
                    allMatchingPairs.add(new Mapping(pModi, bModi, record.get(bModi)));
                }
            }
        }

        // Filter out the pairs whose similarity is less than 0.5
        allMatchingPairs.sort(Comparator.comparing(Mapping::getSimilarity).reversed());
        for(int i =0; i<allMatchingPairs.size(); ++i){
            LevenShteinDistance ld = new LevenShteinDistance(allMatchingPairs.get(i).getBNode().toString(), allMatchingPairs.get(i).getPNode().toString());
            double contentSimilarity = 1.0 - (ld.compute()*1.0 / (allMatchingPairs.get(i).getPNode().toString().length()*1.0));
            if(contentSimilarity < 0.5){
                continue;
            }
            filteredMatchingPairs.add(allMatchingPairs.get(i));
        }

        // Filter out the pairs whose node type & ancestor type are different
        for(int i =0; i<filteredMatchingPairs.size(); ++i){
            Node bNode = filteredMatchingPairs.get(i).getBNode();
            Node pNode = filteredMatchingPairs.get(i).getPNode();
            if(bNode.getNodeType() == pNode.getNodeType()){
                String bAns = getAncestor(bNode);
                String pAns = getAncestor(pNode);
                if(pAns.equals(bAns)){
                    finalMatchingPairs.add(filteredMatchingPairs.get(i));
                }
            }
        }

        // Get the matching pairs whose similarity >=0.4
        for(Mapping mp: finalMatchingPairs){
            if(mp.getSimilarity() < 0.4) break;
            Node pnode = mp.getPNode();
            Node bnode = mp.getBNode();
            if(pMatched1.contains(pnode) || bMatched1.contains(bnode)){
                continue;
            }
            pMatched1.add(pnode);
            bMatched1.add(bnode);
        }

//        pMatched1.sort(Comparator.comparing(Node::getStartLine));
//        bMatched1.sort(Comparator.comparing(Node::getStartLine));
        System.out.println("Generating...");
        LevelLogger.debug("bModifiedStmts:"+bModifiedStmts);
        LevelLogger.debug("pModifiedStmts:"+pModifiedStmts);
        LevelLogger.debug("bMatched1:"+bMatched1);
        LevelLogger.debug("pMatched1:"+pMatched1);

        Generator actionGenerator = new Generator(bAllStmts, pAllStmts, bMatched1, pMatched1, bModifiedStmts, pModifiedStmts, _keepMap, _addBrackets, _deleteBrackets, _checker);
        actionGenerator.generate();
        System.out.println("Generating done!");
        System.out.println(actionGenerator.getActions());

        if (Constant.TOOL_FUNCTION.equals("Diff")) {
            List<MyActions> tmp = actionGenerator.getActions();
            List<List<MyActions>> tmp1 = new ArrayList<>(); tmp1.add(tmp);
            List<List<List<MyActions>>> ret = new ArrayList<>();
            ret.add(tmp1);
            System.out.println("back from diff");
            return ret;
        }
//        JavaFile.writeStringToFile(_logfile, "All action amount is:"+actionGenerator.getActions().size()+"\n", true);
//        return new ArrayList<>();
//        if(actionGenerator.getActions().size() > 20){
//            System.out.println("Too many actions, skip!");
//            return new ArrayList<>();
//        }
//        System.exit(-1);
        // allSelect...
        //        List<MyActions> ret = actionGenerator.allSelect();
//        retList.add(ret);
        // random apply times
//        List<List<MyActions>> randomActions = actionGenerator.randomSelect(100000);
        // one by one select
//        List<List<MyActions>> randomActions = actionGenerator.oneByOneSelect(10000);
//        LevelLogger.debug("Return Actions size:"+randomActions.size());
//        List<List<MyActions>> retList = new ArrayList<>(randomActions);

        // one by one select v2
        List<List<List<MyActions>>> randomActionsV2 = actionGenerator.oneByoneSelectionV2(1000);
        LevelLogger.debug("Return Actions size:"+randomActionsV2.size());
        List<List<List<MyActions>>> retList = new ArrayList<>(randomActionsV2);

        // get actions statistics information
//        List<List<MyActions>> retList = new ArrayList<>();
//        retList.add(actionGenerator.getActions());
        return retList;
//        for(Node bModi: bModifiedStmts){
//            if(!bMatched1.contains(bModi)){
//                System.out.println("DELETE:\n"+bModi.toString());
//            }
//        }
//        for(Node pModi: pModifiedStmts){
//            if(!pMatched1.contains(pModi)){
//                System.out.println("INSERT:\n"+pModi.toString());
//            }
//        }
//        for(int i =0; i<pMatched1.size(); ++i){
//            System.out.println("UPDATE:\nFROM\n"+bMatched1.get(i)+"TO\n"+pMatched1.get(i));
//        }
//        return null;

//        while(!pQueue.isEmpty()){
//            Node pnode = pQueue.poll();
//            if(pnode.getStmtParent()!=null){
//                Node tmpParent = pnode.getStmtParent();
//                if(pParent.containsKey(tmpParent)) pParent.get(tmpParent).add(pnode);
//                else pParent.put(tmpParent, List.of(pnode));
//            }
//            for(Node bnode: bQueue){
//            }
//
//        }

//        System.exit(1);
//        LevelLogger.info("Try match with _level : " + level.name());
//        List<Node> bNodes = new ArrayList<>(buggy.flattenTreeNode(new LinkedList<>()));
//        List<Node> bAllStmts1 = new ArrayList<>(filter(bNodes));
//        bNodes = filterBuggy(bAllStmts);
//        List<Node> bKeep = new ArrayList<>();
//        for(Node node: bAllStmts){
//            if(!bNodes.contains(node)){
//                bKeep.add(node);
//            }
//        }
//
//        List<Node> pNodes = new ArrayList<>(patch.flattenTreeNode(new LinkedList<>()));
//        List<Node> pAllStmts1 = new ArrayList<>(filter(pNodes));
//        pNodes = filterPatch(pAllStmts);
//        List<Node> pKeep = new ArrayList<>();
//        for(Node node: pAllStmts){
//            if(!pNodes.contains(node)){
//                pKeep.add(node);
//            }
//        }
//
//        Map<Node, Node> keep = mapKeep(bKeep, pKeep);
////        System.out.println(bNodes.toString());
//        System.out.println("filtered Done");
////        for(Node i : bNodes){
////            System.out.println("begin: "+i.toString()+i.getTypeStr()+" end.");
////        }
////        System.out.println("Patch nodes:"+pNodes);
//
//        // first Bottom-up match
//        // calculate: control-dependency; data-denpendency; parent nodes
////        Map<Node, Integer> controlCount = new HashMap<>();
////        Map<Node, Integer> dataCount = new HashMap<>();
////        Map<Node, Integer> childrenCount = new HashMap<>();
//
//        bAllStmts.sort(Comparator.comparing(Node::getChildrenNum));
//        pAllStmts.sort(Comparator.comparing(Node::getChildrenNum));
//        List<Map<Node, Integer>> info = getChildrenFeature(bAllStmts, pAllStmts);
//        Map<Node, Integer> childrenCount = info.get(0);
////        for(Node node: bAllStmts){
////            if(node.getControldependency()!=null){
////                if(!controlCount.containsKey(node.getControldependency())){
////                    controlCount.put(node.getControldependency(), 1);
////                }else{
////                    controlCount.put(node.getControldependency(), controlCount.get(node.getControldependency())+1);
////                }
////            }
////            if(node.getDataDependency()!=null){
////                if(!dataCount.containsKey(node.getDataDependency())){
////                    dataCount.put(node.getDataDependency(), 1);
////                }else{
////                    dataCount.put(node.getDataDependency(), dataCount.get(node.getDataDependency())+1);
////                }
////            }
////            if(node.getParent() !=null){
////                if(!childrenCount.containsKey(node.getParent())){
////                    childrenCount.put(node.getParent(), 1);
////                }else{
////                    childrenCount.put(node.getParent(), childrenCount.get(node.getParent())+1);
////                }
////            }
////        }
////
////        for(Node node: pAllStmts){
////            if(node.getControldependency()!=null){
////                if(!controlCount.containsKey(node.getControldependency())){
////                    controlCount.put(node.getControldependency(), 1);
////                }else{
////                    controlCount.put(node.getControldependency(), controlCount.get(node.getControldependency())+1);
////                }
////            }
////            if(node.getDataDependency()!=null){
////                if(!dataCount.containsKey(node.getDataDependency())){
////                    dataCount.put(node.getDataDependency(), 1);
////                }else{
////                    dataCount.put(node.getDataDependency(), dataCount.get(node.getDataDependency())+1);
////                }
////            }
////            if(node.getParent() !=null){
////                if(!childrenCount.containsKey(node.getParent())){
////                    childrenCount.put(node.getParent(), 1);
////                }else{
////                    childrenCount.put(node.getParent(), childrenCount.get(node.getParent())+1);
////                }
////            }
////        }
//        // second: top to down
//        // get top -> node's struct and node's location
//        System.out.println("Top -> Down!!!");
//        bNodes.sort(Comparator.comparing(Node::getStartLine).thenComparing(Comparator.comparing(Node::getChildrenNum).reversed()));
//        pNodes.sort(Comparator.comparing(Node::getStartLine).thenComparing(Comparator.comparing(Node::getChildrenNum).reversed()));
//
//        Map<Node, List<Node.TYPE>> topStruct = new HashMap<>();
//        Map<Node, Integer> position = new HashMap<>();
//        for(Node node: bNodes){
//            int index = bNodes.indexOf(node);
//            List<Node.TYPE> path = new ArrayList<>();
//            Node tmp =  node;
//            while (tmp.getParent()!=null){
//                path.add(tmp.getParent().getNodeType());
//                tmp = tmp.getParent();
//            }
//            Collections.reverse(path);
//            topStruct.put(bNodes.get(index), path);
//        }
//
//        for(Node node: pNodes){
//            int index = pNodes.indexOf(node);
//            List<Node.TYPE> path = new ArrayList<>();
//            while (node.getParent()!=null){
//                path.add(node.getParent().getNodeType());
//                node = node.getParent();
//            }
//            Collections.reverse(path);
//            topStruct.put(pNodes.get(index), path);
//        }
////        System.out.println(topStruct);
//        // Matching
//        Map<Node, Node> update = new HashMap<>();
//        List<Integer> bMatched = new ArrayList<>();
//        List<Integer> pMatched = new ArrayList<>();
//        List<Node> insert = new ArrayList<>();
//        List<Node> delete = new ArrayList<>();
////        System.out.println("ready to match, Buggy:"+bNodes);
////        System.out.println("ready to match, Patch:"+pNodes);
//        for(Node pnode: pNodes){
//            for(Node bnode: bNodes){
//                if(matchCondition(pnode, bnode, topStruct, childrenCount)){
//                    bMatched.add(bNodes.indexOf(bnode));
//                    pMatched.add(pNodes.indexOf(pnode));
//                    break;
//                }
//            }
//        }
//        assert bMatched.size() == pMatched.size();
//        System.out.println(pMatched);
//        for(int i =0; i<bNodes.size(); ++i){
//            if(bMatched.contains(i)) continue;
//            delete.add(bNodes.get(i));
//            System.out.println("DELETE:\n"+delete.get(delete.size()-1).toString()+"\n");
//        }
//        for(int i =0; i<pNodes.size(); ++i){
//            if(pMatched.contains(i)) continue;
//            insert.add(pNodes.get(i));
////            System.out.println("INSERT:\n"+insert.get(insert.size()-1).toString()+"\n");
//            System.out.println("INSERT:\n"+insert.get(insert.size()-1).toString()+"\n"+"POS:\n"+getInsertPos(pNodes.get(i), pAllStmts, keep, insert, update));
//        }
//        for(int i=0; i<bMatched.size(); ++i){
//            int bindex = bMatched.get(i);
//            int pindex = pMatched.get(i);
//            System.out.println(bindex+";"+pindex);
//            update.put(pNodes.get(pindex), bNodes.get(bindex));
//            System.out.println("UPDATE:\nFrom\n"+bNodes.get(bindex).getStartLine()+":"+bNodes.get(bindex)+"\n"+"To\n"+pNodes.get(pindex).getStartLine()+":"+pNodes.get(pindex).toString()+"\n");
//        }
//
//        for(Node inode: insert){
//            getIndex(inode);
//            break;
//        }
//
//        System.exit(-1);
    }

}
