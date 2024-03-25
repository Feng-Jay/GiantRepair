

package mfix.core.node.match.metric;


public abstract class AbsSimilarity implements IScore {

    private double _weight;

    public AbsSimilarity(double weight) {
        _weight = weight;
    }

    @Override
    public double getWeight() {
        return _weight;
    }

    @Override
    public void setWeight(double weight) {
        _weight = weight;
    }
}
