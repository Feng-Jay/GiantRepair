

package mfix.common.java;

public interface IExecute {

    boolean compile();
    boolean test();
    boolean test(String testcase);
    boolean test(String clazz, String method);

}
