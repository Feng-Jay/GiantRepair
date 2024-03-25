

package mfix.core.node.diff.text;

import mfix.common.conf.Constant;
import mfix.core.node.diff.Add;

import java.io.Serializable;


public class AddLine extends Line implements Add, Serializable {

	private static final long serialVersionUID = -5178723884749316617L;

	public AddLine(String text) {
		super(text);
		_leading = Constant.PATCH_ADD_LEADING;
	}
}
