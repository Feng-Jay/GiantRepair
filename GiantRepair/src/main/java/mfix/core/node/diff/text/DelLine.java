

package mfix.core.node.diff.text;

import mfix.common.conf.Constant;
import mfix.core.node.diff.Delete;

import java.io.Serializable;


public class DelLine extends Line implements Delete, Serializable {

	private static final long serialVersionUID = 7207850128129029810L;

	public DelLine(String text) {
		super(text);
		_leading = Constant.PATCH_DEL_LEADING;
	}
	
}
