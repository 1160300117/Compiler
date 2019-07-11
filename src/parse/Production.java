package parse;

import java.util.List;

public class Production {

	int no;					// 序号
	String left;			// 左部
	String[] right;			// 右部
	List<String> select;	// 后继搜索符

	public String getError() {
		return null;
	}

	public String getSolution() {
		return null;
	}

	
	@Override
	public String toString() {
		String ret = left + "  -->  ";

		if (right.length == 0)
			ret += " ξ";

		for (int i = 0; i < right.length; i++) {
			ret += "  " + right[i];
		}

		return ret;
	}

	public List<String> getSelect() {
		return select;
	}

	public void setSelect(List<String> select) {
		this.select = select;
	}

	public int getNo() {
		return no;
	}

	public String getLeft() {
		return left;
	}

	public String[] getRight() {
		return right;
	}

	public Production() {
	}

	public Production(int no, String left) {
		this.no = no;
		this.left = left;
		this.right = new String[0];
	}

	public Production(int no, String left, String[] right) {
		this.no = no;
		this.left = left;
		this.right = new String[1];
		this.right = right;
	}
}
