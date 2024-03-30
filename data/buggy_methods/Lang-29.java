public class tmp {
  static float toJavaVersionInt(  String version){
    return toVersionInt(toJavaVersionIntArray(version,JAVA_VERSION_TRIM_SIZE));
  }
}
