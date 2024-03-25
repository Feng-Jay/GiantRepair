

package mfix.common.java;

import java.util.List;

public class FakeSubject extends Subject {

    public final static String NAME = "FakeSubject";
    private List<String> _filesToRepair;

    public FakeSubject(String base, List<String> filesToRepair) {
        super(base, NAME, "", "", "", "");
        _type = NAME;
        _filesToRepair = filesToRepair;
        _compile_file = true;
    }

    public List<String> getBuggyFiles() {
        return _filesToRepair;
    }

}
