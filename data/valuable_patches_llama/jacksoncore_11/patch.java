public class tmp {
  private void _verifySharing(){
    if (_hashShared) {
      _hashArea=Arrays.copyOf(_hashArea,_hashArea.length);
      _names=Arrays.copyOf(_names,_names.length);
      _hashShared=false;
      rehash();
    }
    if (_needRehash) {
      rehash();
    }
  }
}
