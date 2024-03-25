package mfix.core.node.match;

import mfix.core.node.ast.Node;
import mfix.core.node.match.metric.LevenShteinDistance;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Mapping {
    private Node _pNode;

    private List<Node> _pNeighbors;
    private Node _bNode;

    private List<Node> _bNeighbors;

    private Integer _bIndex;
    private Integer _pIndex;
    private double _similarity;

    public Mapping(Node pNode, Node bNode, List<Node> pNeighbors, List<Node> bNeighbors, int bIndex, int pIndex){
        _pNode = pNode;
        _bNode = bNode;
        _pNeighbors = pNeighbors;
        _bNeighbors = bNeighbors;
        _bIndex = bIndex;
        _pIndex = pIndex;
        computeSim();
    }
    public Mapping(Node pNode, Node bNode, double sim){
        _pNode = pNode;
        _bNode = bNode;
        _similarity = sim;
    }

    public double getSimilarity(){return _similarity;}

    public Node getPNode(){return _pNode;}
    public Node getBNode(){return _bNode;}
    private void computeSim(){
        if(!_pNode.getNodeType().equals(_bNode.getNodeType())){
            _similarity = 0.0;
            return;
        }
        // structure, context and content similarity

        // structure: the index of node's parent
        double structScore = 1.0 - (Math.abs(_bIndex - _pIndex)*1.0/_pIndex);
        LevenShteinDistance ldDistance = new LevenShteinDistance(_pNode.toString(), _bNode.toString());
        double contentSimilarity = 1.0 - (ldDistance.compute()*1.0 / (_pNode.toString().length()*1.0));

        double contextSimilarity = 0;
        for(int i=0; i<_bNeighbors.size(); ++i){
            ldDistance = new LevenShteinDistance(_bNeighbors.get(i).toString(), _pNeighbors.get(i).toString());
            double tmp = 1.0 - (ldDistance.compute()*1.0 / (_pNeighbors.get(i).toString().length()*1.0));
            contextSimilarity += tmp;
        }
        contextSimilarity = contextSimilarity / _bNeighbors.size();

        _similarity = 0.5*contentSimilarity + 0.3 * contextSimilarity + 0.2 * structScore;
    }

    public static double computeSim(Node pNode, Node bNode, List<Node> pNeighbors, List<Node> bNeighbors, int pIndex, int bIndex){
        if(!pNode.getNodeType().equals(bNode.getNodeType())){
            return 0.0;
        }
        // structure, context and content similarity

        // structure: the index of node's parent
        double structScore = 1.0 - (Math.abs(bIndex - pIndex)*1.0/pIndex);
        structScore = Math.max(0, structScore);
        LevenShteinDistance ldDistance = new LevenShteinDistance(pNode.toString(), bNode.toString());
        double contentSimilarity = 1.0 - (ldDistance.compute()*1.0 / (bNode.toString().length()*1.0));
        contentSimilarity = Math.max(0, contentSimilarity);
        double contextSimilarity = 0;
        for(int i=0; i<bNeighbors.size(); ++i){
            double tmp = 0;
            if(bNeighbors.get(i) != null && pNeighbors.get(i) !=null)
            {
                ldDistance = new LevenShteinDistance(bNeighbors.get(i).toString(), pNeighbors.get(i).toString());
                tmp = 1.0 - (ldDistance.compute()*1.0 / (bNeighbors.get(i).toString().length()*1.0));
            }
            contextSimilarity += tmp;
        }
        contextSimilarity = contentSimilarity / bNeighbors.size();
        contextSimilarity = Math.max(0, contextSimilarity);
        return 0.6*contentSimilarity + 0.3 * contextSimilarity + 0.1 * structScore;
    }
}
