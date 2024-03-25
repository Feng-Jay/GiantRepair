

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;

import java.util.Map;

public interface IScore {

    double getWeight();

    void setWeight(double weight);

    double computeScore(Map<Node, Node> nodeMap, Map<String, String> strMap);

}
