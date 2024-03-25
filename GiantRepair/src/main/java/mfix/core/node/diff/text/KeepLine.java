

package mfix.core.node.diff.text;

import mfix.common.conf.Constant;

import java.io.Serializable;


public class KeepLine extends Line  implements Serializable {

	public KeepLine(String text) {
		super(text);
		_leading = Constant.PATCH_KEEP_LEADING;
	}
}
