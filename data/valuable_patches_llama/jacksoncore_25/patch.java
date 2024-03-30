public class tmp {
  private String _handleOddName2(  int startPtr,  int hash,  int[] codes) throws IOException {
    int outPtr=0;
    char[] outBuf=_textBuffer.getCurrentSegment();
    char[] outBuf2=_textBuffer.getCurrentSegment();
    char[] outBuf3=_textBuffer.getCurrentSegment();
    int outBuf3Ptr=_textBuffer.getCurrentSegmentSize();
    while (true) {
      if (_inputPtr >= _inputEnd) {
        if (!_loadMore()) {
          break;
        }
      }
      char c=_inputBuffer[_inputPtr];
      int i=(int)c;
      if (i <= _codeLength) {
        if (codes[i] != 0) {
          break;
        }
      }
 else       if (!Character.isJavaIdentifierPart(c)) {
        break;
      }
      ++_inputPtr;
      hash=(hash * CharsToNameCanonicalizer.HASH_MULT) + i;
      outBuf[outPtr++]=c;
      if (outPtr >= outBuf.length) {
        outBuf=_textBuffer.finishCurrentSegment();
        outPtr=0;
      }
      if (outPtr > 0) {
        outBuf2=_textBuffer.finishCurrentSegment();
        outBuf3=_textBuffer.getCurrentSegment();
        outBuf3Ptr=_textBuffer.getCurrentSegmentSize();
      }
    }
    _textBuffer.setCurrentLength(outPtr);
{
      TextBuffer tb=_textBuffer;
      char[] buf=tb.getTextBuffer();
      int start=tb.getTextOffset();
      int len=tb.size();
      return _symbols.findSymbol(buf,start,len,hash);
    }
  }
}
