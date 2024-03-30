public class tmp {
  static boolean mayBeString(  Node n,  boolean recurse){
    if (recurse) {
      return allResultsMatch(n,MAY_BE_STRING_PREDICATE,true);
    }
 else {
      return mayBeStringHelper(n,true);
    }
  }
}
