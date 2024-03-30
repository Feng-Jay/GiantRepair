public class tmp {
  Element insert(  Token.StartTag startTag){
    if (startTag.isSelfClosing()) {
      Element el=insertEmpty(startTag);
      stack.add(el);
      if (parser.isFragmentParsing() && parser.state() == State.InBody) {
        tokeniser.transition(TokeniserState.Data);
      }
 else {
        tokeniser.emit(new Token.EndTag(el.tagName()));
      }
      return el;
    }
    Element el=new Element(Tag.valueOf(startTag.name()),baseUri,startTag.attributes);
    insert(el);
    return el;
  }
}
