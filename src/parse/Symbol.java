package parse;

import java.util.ArrayList;
import java.util.List;

public class Symbol {
	int num; 						// 符号序号
	private String name;			// 符号值
	private char type; 				// N或T
	private String higher_name; 	// 高层结构的符号
	public List<String> first; 		// First集
	public List<String> follow; 	// Follow集

	public Symbol(int num, String name, char type) {
		super();
		this.num = num;
		this.name = name;
		this.type = type;
		this.first = new ArrayList<String>();

		if (type == 'N') {
			this.follow = new ArrayList<String>();
		} else if (type == 'T') {
			this.first.add(name);
			this.follow = null;
		} else {
			throw new IllegalArgumentException("非法的符号类型");
		}
	}

	public Symbol(int num, String name, char type, String higher_name) {
		this(num, name, type);
		this.higher_name = higher_name;
	}

	public String getName() {
		return name;
	}

	public String getHigher_name() {
		return higher_name;
	}

	public boolean isTerminal() {
		return type == 'T';
	}

	// 判断First集或Follow集中是否含有某符号
	public boolean has(String arr_name, String sym_name) {
		List<String> arr = null;
		if (arr_name.toUpperCase().equals("FIRST")) {
			arr = this.first;
		} else if (arr_name.toUpperCase().equals("FOLLOW")) {
			arr = this.follow;
		} else {
			throw new IllegalArgumentException("非法的集合名");
		}
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i).equals(sym_name))
				return true;
		}
		return false;
	}
}
