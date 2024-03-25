

package mfix.core.node.match.metric;

import mfix.core.node.ast.Node;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocationScore extends AbsSimilarity {

    private List<Integer> _buggyLines;

    public LocationScore(double weight, List<Integer> buggyLines) {
        super(weight);
        _buggyLines = buggyLines;
    }

    @Override
    public double computeScore(Map<Node, Node> nodeMap, Map<String, String> strMap) {
        if (_buggyLines == null || _buggyLines.isEmpty()) {
            return 1.0;
        }
        Set<Integer> lines = new HashSet<>();
        for (Map.Entry<Node, Node> entry : nodeMap.entrySet()) {
            lines.add(entry.getValue().getStartLine());
            if (entry.getValue().getParentStmt() != null) {
                lines.add(entry.getValue().getParentStmt().getStartLine());
            }
        }
        for (int i = 0; i < _buggyLines.size(); i++) {
            if (lines.contains(_buggyLines.get(i))) {
                return 1.0 / Math.sqrt((i + 1.0));
            }
        }
        return 0;
    }
}
