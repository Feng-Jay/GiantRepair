public class tmp {
  public List<String> getMatchingOptions(  String opt){
    opt=Util.stripLeadingHyphens(opt);
    List<String> matchingOpts=new ArrayList<String>();
    for (    String longOpt : longOpts.keySet()) {
      if (longOpt.startsWith(opt)) {
        matchingOpts.add(longOpt);
      }
    }
    return matchingOpts;
  }
}
