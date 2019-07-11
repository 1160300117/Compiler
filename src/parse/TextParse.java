package parse;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.Stack;

import javax.swing.table.DefaultTableModel;

import lexcial.Token;

public class TextParse {
	List<Production> productions;
	List<Symbol> symbols;
	List<Id> ids;// 标识符表
	List<String> codes; // 三地址码
	DefaultTableModel tbmodel_parse_result;
	DefaultTableModel tbmodel_error;

	public List<Id> getIds() {
		return ids;
	}

	public List<String> getCodes() {
		return codes;
	}

	public TextParse(DefaultTableModel tbmodel_parse_result, DefaultTableModel tbmodel_error) {
		this.tbmodel_parse_result = tbmodel_parse_result;
		this.tbmodel_error = tbmodel_error;
		ids = new ArrayList<Id>();
		codes = new ArrayList<String>();
		symbols = new ArrayList<Symbol>();
		productions = new ArrayList<Production>();
		getProduction();
		getFirst();
		getFollow();
		getSelect();
	}

	public void getProduction() {
		// 改造文法
		try {
			String line;
			int num;
			char type;
			String left;
			String[] data;
			String[] right;

			// 获取产生式
			File file_production = new File("production.txt");
			RandomAccessFile rf_production = new RandomAccessFile(file_production, "r");
			while ((line = rf_production.readLine()) != null) {
				data = line.split("-->");
				num = Integer.parseInt(data[0].split(" ")[0]);
				left = data[0].split(" ")[1].trim();
				if (data.length < 2) {
					productions.add(new Production(num, left));
				} else {
					right = data[1].trim().split(" ");
					productions.add(new Production(num, left, right));
				}
			}
			rf_production.close();

			// 获取符号集
			File file_symbol = new File("symbol.txt");
			RandomAccessFile rf_symbol = new RandomAccessFile(file_symbol, "r");
			while ((line = rf_symbol.readLine()) != null) {
				data = line.split(" ");
				num = Integer.parseInt(data[0]);
				type = data[2].charAt(0);
				symbols.add(new Symbol(num, data[1], type));
			}
			rf_symbol.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 获取first集
	public void getFirst() {
		boolean flag = true;
		while (flag) {
			for (Production production : productions) {
				Production temp = production;
				String left = temp.getLeft();
				String[] right = temp.getRight();
				Symbol left_symbol = getSymbol(left);
				// 找到第一个不为空的符号
				for (int i = 0; i < right.length; i++) {
					Symbol right_symbol = getSymbol(right[i]);
					for (String element : right_symbol.first) {
						if (!left_symbol.has("first", element)) {
							left_symbol.first.add(element);
							flag = false;
						}
					}
					if (!canBeBlank(right[i])) {
						break;
					}
				}
			}
			flag = !flag;
		}
	}

	// 获取follow集
	public void getFollow() {
		getSymbol("S").follow.add("#");
		boolean flag = true;
		while (flag) {
			for (Production production : productions) {
				Production temp = production;
				String left = temp.getLeft();
				String[] right = temp.getRight();
				if (right.length == 0) {
					continue;
				}
				Symbol left_symbol = getSymbol(left);
				// 依次对每一个右部非终结符进行处理
				for (int i = 0; i < right.length - 1; i++) {
					Symbol right_symbol = getSymbol(right[i]);
					if (right_symbol.isTerminal()) {
						continue;
					}
					Symbol follow_symbol = getSymbol(right[i + 1]);
					for (String element : follow_symbol.first) {
						if (!right_symbol.has("follow", element)) {
							right_symbol.follow.add(element);
							flag = false;
						}
					}
					// 此非终结符右边推出空
					boolean blank = true;
					for (int j = i + 1; j < right.length; j++) {
						if (canBeBlank(right[j])) {
							if (j + 1 < right.length) {
								Symbol rr_symbol = getSymbol(right[j + 1]);
								for (String element : rr_symbol.first) {
									if (!right_symbol.has("follow", element)) {
										right_symbol.follow.add(element);
										flag = false;
									}
								}
							}
						} else {
							blank = false;
							break;
						}
					}
					if (blank) {
						for (String element : left_symbol.follow) {
							if (!right_symbol.has("follow", element)) {
								right_symbol.follow.add(element);
								flag = false;
							}
						}
					}
				}
				// 处理产生式右部最后一个非终结符
				Symbol last_symbol = getSymbol(right[right.length - 1]);
				if (last_symbol.isTerminal()) {
					continue;
				}
				for (String element : left_symbol.follow) {
					if (!last_symbol.has("follow", element)) {
						last_symbol.follow.add(element);
						flag = false;
					}
				}
			}
			flag = !flag;
		}

	}

	// 获取select集
	public void getSelect() {
		for (Production production : productions) {
			// 产生式右部为空
			if (production.getRight().length == 0) {
				List<String> select = new ArrayList<String>();
				List<String> follow = getSymbol(production.getLeft()).follow;
				for (int i = 0; i < follow.size(); i++) {
					select.add(follow.get(i));
				}
				production.setSelect(select);
			} else {
				List<String> select = new ArrayList<String>();
				String[] right = production.getRight();
				boolean blank = true;
				// 找到第一个不推出空的符号
				for (int i = 0; i < right.length; i++) {
					List<String> first = getSymbol(right[i]).first;
					for (String f : first) {
						select.add(f);
					}
					if (!canBeBlank(right[i])) {
						blank = false;
						break;
					}
				}
				// 产生式能推出空
				if (blank) {
					List<String> follow = getSymbol(production.getLeft()).follow;
					for (String f : follow) {
						if (!select.contains(f)) {
							select.add(f);
						}
					}
				}
				production.setSelect(select);
			}
		}
	}

	public List<Production> getProductions() {
		return productions;
	}

	public List<Production> getProductionsByLeft(String left) {
		List<Production> ret = new ArrayList<Production>();

		for (int i = 0; i < this.productions.size(); i++) {
			Production temp = this.productions.get(i);
			if (temp.getLeft().equals(left))
				ret.add(temp);
		}

		return ret;
	}

	private boolean canBeBlank(String name) {
		for (int i = 0; i < this.productions.size(); i++) {
			Production temp = this.productions.get(i);
			if (temp.getLeft().equals(name)) {
				String[] right = temp.getRight();
				if (right.length == 0) {
					return true;
				}
				boolean flag = true;
				for (int j = 0; j < right.length; j++) {
					if (!canBeBlank(right[j])) {
						flag = false;
						break;
					}
				}
				if (flag) {
					return true;
				}
			}
		}

		return false;
	}

	// 找到能推出空的产生式
	private Production getProductionToBlank(String name) {
		for (int i = 0; i < this.productions.size(); i++) {
			Production temp = this.productions.get(i);
			if (temp.getLeft().equals(name)) {
				String[] right = temp.getRight();
				if (right.length == 0) {
					return temp;
				}
				boolean flag = true;
				for (int j = 0; j < right.length; j++) {
					if (!canBeBlank(right[j])) {
						flag = false;
						break;
					}
				}
				if (flag) {
					return temp;
				}
			}
		}

		return null;
	}

	public List<Symbol> getSymbols() {
		return symbols;
	}

	Symbol getSymbol(String name) {
		for (Symbol symbol : symbols) {
			Symbol temp = symbol;
			if (temp.getName().equals(name))
				return temp;
		}
		return null;
	}

	private Id getId(String name) {
		for (Id id : ids) {
			if (id.getName().equals(name))
				return id;
		}
		return null;
	}

	// 分析标识符在内存中的偏移
	public List<Production> analysis(List<Token> token_list) {

		int offset = 0; // 标识符在内存中的偏移
		int t_num = 0; // 中间变量的个数
		int b_num = 0; // 布尔变量的个数

		token_list.add(new Token("#", null));

		Stack<Symbol> stack = new Stack<Symbol>(); // 符号栈
		Stack<Node> node_stack = new Stack<Node>();
		List<Production> pro_list = new ArrayList<Production>(); // 产生式顺序

		stack.push(getSymbol("#")); // 栈底
		stack.push(getSymbol("S"));

		node_stack.push(new Node("S", null));

		int pos = 0; // 已匹配数目
		int line = 1; // 行号

		while (pos < token_list.size()) {
			Token token = token_list.get(pos);
			Symbol input_symbol = getSymbol(token.getName());

			if (input_symbol == null) { // 该文法不能识别的输入符号
				if (token.getName().equals("ENTER")) {
					line++;
				} else if (token.getName().equals("ERROR")) {
					tbmodel_error.addRow(new String[]{ line + "", 
							"无法识别的单词\'" + token.getSource() + "\'", 
							"跳过错误单词\'" + token.getSource() + "\'"});
				} else {
					tbmodel_error.addRow(new String[]{ line + "", 
							"无法识别的输入符号\'" + token.getName() + "\'", 
							"跳过输入符号\'" + token.getName() + "\'"});
				}
				pos++;
				continue;
			}

			Symbol leftest = null;
			Node left_node = null;
			try {
				leftest = stack.pop();
				if (!leftest.isTerminal()) {
					left_node = node_stack.pop();
				}
			} catch (EmptyStackException e) { // 符号栈已空，输入未结束
				tbmodel_error.addRow(new String[]{ line + "", "符号栈已空，输入栈仍然有字符存在", "句法分析终止" });
				break;
			}

			// 赋值语句
			if (leftest.getName().equals("M68_2")) { // 准备接收CHAR
				left_node.getFather().attribute.put("type", "char");
				left_node.getFather().attribute.put("length", "1");
			} else if (leftest.getName().equals("M69_2")) { // 准备接收INT
				left_node.getFather().attribute.put("type", "int");
				left_node.getFather().attribute.put("length", "4");
			} else if (leftest.getName().equals("M70_2")) { // 准备接收LONG
				left_node.getFather().attribute.put("type", "long");
				left_node.getFather().attribute.put("length", "4");
			} else if (leftest.getName().equals("M71_2")) { // 准备接收SHORT
				left_node.getFather().attribute.put("type", "short");
				left_node.getFather().attribute.put("length", "2");
			} else if (leftest.getName().equals("M72_2")) { // 准备接收FLOAT
				left_node.getFather().attribute.put("type", "float");
				left_node.getFather().attribute.put("length", "4");
			} else if (leftest.getName().equals("M73_2")) { // 准备接收DOUBLE
				left_node.getFather().attribute.put("type", "double");
				left_node.getFather().attribute.put("length", "8");
			} else if (leftest.getName().equals("M13_2")) { // 接收完一个变量
				Node father = left_node.getFather();
				father.sons.get(1).attribute.put("name", token_list.get(pos - 1).getSource());
				father.sons.get(1).attribute.put("type", father.sons.get(0).attribute.get("type"));
				father.sons.get(1).attribute.put("length", father.sons.get(0).attribute.get("length"));
				father.sons.get(1).attribute.put("dimension", "0");
			} else if (leftest.getName().equals("M15_4")) { // 接收数组尺寸
				Node father = left_node.getFather();
				int num = Integer.parseInt(token_list.get(pos - 2).getValue());
				int father_dimension = Integer.parseInt(father.attribute.get("dimension"));
				father.sons.get(0).attribute.put("name", father.attribute.get("name"));
				father.sons.get(0).attribute.put("type", father.attribute.get("type"));
				if (father.attribute.get("length") != null) {
					father.sons.get(0).attribute.put("length",
							Integer.parseInt(father.attribute.get("length")) * num + "");
				}
				father.sons.get(0).attribute.put("dimension", (father_dimension + 1) + "");
				father.sons.get(0).attribute.put("arr" + father_dimension, "" + num);
				for (int i = 0; i < father_dimension; i++) {
					father.sons.get(0).attribute.put("arr" + i, "" + father.attribute.get("arr" + i));
				}
			} else if (leftest.getName().equals("M14_1")) { // 接收变量
				Node father = left_node.getFather();
				if (father.attribute.get("length") != null) { // 定义
					int length = Integer.parseInt(father.attribute.get("length"));
					String name = father.attribute.get("name");
					for (Id i : ids) {
						if (i.name.equals(father.attribute.get("name"))) {
							tbmodel_error.addRow(new String[]{ line + "", "变量名重复", "重命名" });
							name = name + "`";
							break;
						}
					}
					Id id = new Id(name, father.attribute.get("type"), offset, length);
					offset += length;
					int dimension = Integer.parseInt(father.attribute.get("dimension"));
					for (int i = 0; i < dimension; i++) {
						id.arr_list.add(Integer.parseInt(father.attribute.get("arr" + i)));
					}
					ids.add(id);
				} else { // 数组执行
					String name = father.attribute.get("name");
					Id id = getId(name);
					String type = id.getType();
					int dimension = id.arr_list.size(); // 维数

					int ofst = 0; // 偏移元素个数
					int width = 1; // 各维宽度

					for (int i = dimension - 1; i >= 0; i--) {
						int arr = Integer.parseInt(father.attribute.get("arr" + i));
						ofst += arr * width;
						width *= id.arr_list.get(i);
					}

					if (id.arr_list.size() > 0) { // 数组元素
						String t = "t" + (t_num++);
						if (ofst > Integer.parseInt(father.attribute.get("dimension"))) {
							tbmodel_error.addRow(new String[]{ line + "", "数组越界", "跳过产生式" });
							continue;
						} else {
							if (type.equals("int") || type.equals("long") || type.equals("float")) {
								ofst *= 4;
							} else if (type.equals("double")) {
								ofst *= 8;
							} else if (type.equals("short")) {
								ofst *= 2;
							}
							codes.add(t + " := " + name + "[" + ofst + "]");
							father.attribute.put("value", t);
							father.attribute.put("val", name + "[" + ofst + "]");
						}
					} else { // 单独变量
						father.attribute.put("value", name);
					}
				}
			} else if (leftest.getName().equals("M13_4")) { // 一个数组接收完毕
				Node father = left_node.getFather();
				father.sons.get(2).attribute.put("type", father.sons.get(0).attribute.get("type"));
				father.sons.get(2).attribute.put("length", father.sons.get(0).attribute.get("length"));
			} else if (leftest.getName().equals("M17_3")) { // 接收完下一个变量
				Node father = left_node.getFather();
				father.sons.get(0).attribute.put("type", father.attribute.get("type"));
				father.sons.get(0).attribute.put("length", father.attribute.get("length"));
				father.sons.get(0).attribute.put("name", token_list.get(pos - 1).getSource());
				father.sons.get(0).attribute.put("dimension", "0");
			} else if (leftest.getName().equals("M17_5")) { // 下一个数组接收完毕
				Node father = left_node.getFather();
				father.sons.get(1).attribute.put("type", father.sons.get(0).attribute.get("type"));
				father.sons.get(1).attribute.put("length", father.sons.get(0).attribute.get("length"));
			} else if (leftest.getName().equals("M74_2")) { // 字符常量
				Node father = left_node.getFather();
				father.attribute.put("value", token_list.get(pos - 1).getValue());
			} else if (leftest.getName().equals("M57_3")) { // 布尔表达式：!
				Node father = left_node.getFather();
				String f1 = "b" + (b_num++);
				String f2 = father.sons.get(0).attribute.get("value");
				codes.add(f1 + " := ~" + f2);
				father.attribute.put("value", f1);
			} else if (leftest.getName().equals("M58_3")) { // 处理++
				Node father = left_node.getFather();
				String f1 = "t" + (t_num++);
				String f2 = father.sons.get(0).attribute.get("value");
				codes.add(f1 + " := " + f2 + " + 1");
				father.attribute.put("value", f1);
			} else if (leftest.getName().equals("M59_3")) { // 处理--
				Node father = left_node.getFather();
				String f1 = "t" + (t_num++);
				String f2 = father.sons.get(0).attribute.get("value");
				codes.add(f1 + " := " + f2 + " - 1");
				father.attribute.put("value", f1);
			} else if (leftest.getName().equals("M84_3")) { // 处理负数
				Node father = left_node.getFather();
				String f1 = "t" + (t_num++);
				String f2 = father.sons.get(0).attribute.get("value");
				codes.add(f1 + " := 0 - " + f2);
				father.attribute.put("value", f1);
			} else if (leftest.getName().equals("M60_4")) { // 接收一对括号
				Node father = left_node.getFather();
				father.attribute.put("value", father.sons.get(0).attribute.get("value"));
			} else if (leftest.getName().equals("M61_2")) { // 调用数组元素
				Node father = left_node.getFather();
				father.sons.get(0).attribute.put("name", token_list.get(pos - 1).getSource());
			} else if (leftest.getName().equals("M63_1")) { // 接收数组元素
				Node father = left_node.getFather();
				father.sons.get(0).attribute.put("name", father.attribute.get("name"));
				father.sons.get(0).attribute.put("dimension", "0");
			} else if (leftest.getName().equals("M15_6") || leftest.getName().equals("M63_3")
					|| leftest.getName().equals("M61_4") || leftest.getName().equals("M62_2")) { // 接收数组元素完毕
				Node father = left_node.getFather();
				father.attribute.put("value", father.sons.get(0).attribute.get("value"));
				father.attribute.put("val", father.sons.get(0).attribute.get("val"));
			} else if (leftest.getName().equals("M52_2")) { // 准备接受算符
				Node father = left_node.getFather();
				father.sons.get(1).attribute.put("value", father.sons.get(0).attribute.get("value"));
				father.sons.get(1).attribute.put("val", father.sons.get(0).attribute.get("val"));
			} else if (leftest.getName().equals("M54_3")) { // 接收乘法
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String t = "t" + (t_num++);
				codes.add(t + " := " + inh + " * " + value);
				father.sons.get(1).attribute.put("value", t);
			} else if (leftest.getName().equals("M55_3")) { // 接收除法
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String t = "t" + (t_num++);
				codes.add(t + " := " + inh + " / " + value);
				father.sons.get(1).attribute.put("value", t);
			} else if (leftest.getName().equals("M56_3")) { // 接收求余
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String t = "t" + (t_num++);
				codes.add(t + " := " + inh + " % " + value);
				father.sons.get(1).attribute.put("value", t);
			} else if (leftest.getName().equals("M50_3")) { // 接收加法
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String t = "t" + (t_num++);
				codes.add(t + " := " + inh + " + " + value);
				father.sons.get(1).attribute.put("value", t);
			} else if (leftest.getName().equals("M51_3")) { // 接收减法
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String t = "t" + (t_num++);
				codes.add(t + " := " + inh + " - " + value);
				father.sons.get(1).attribute.put("value", t);
			} else if (leftest.getName().equals("M52_4")) { // 算式接收完毕
				Node father = left_node.getFather();
				father.attribute.put("value", father.sons.get(1).attribute.get("value"));
				father.attribute.put("val", father.sons.get(1).attribute.get("val"));
			} else if (leftest.getName().equals("M42_4")) { // 布尔表达式：<
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " < " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M43_4")) { // 布尔表达式：<=
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " <= " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M44_4")) { // 布尔表达式：>
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " > " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M45_4")) { // 布尔表达式：>=
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " >= " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M46_4")) { // 布尔表达式：==
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " == " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M47_4")) { // 布尔表达式：!=
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				String b = "b" + (b_num++);
				codes.add(b + " := " + inh + " != " + value);
				father.attribute.put("value", b);
			} else if (leftest.getName().equals("M78_3")) { // =赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("val");
				String value = father.sons.get(0).attribute.get("value");
				if (inh == null || inh.equals("null"))
					inh = father.attribute.get("value");
				else {
					String temp = father.attribute.get("value");
					for (int i = codes.size() - 1; i >= 0; i--) {
						// 删除一系列中间表达式
						if (codes.get(i) != null && codes.get(i).startsWith(temp)) {
							codes.remove(i);
						}
					}
				}
				codes.add(inh + " := " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M79_3")) { // +=赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				codes.add(inh + " := " + inh + " + " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M80_3")) { // -=赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				codes.add(inh + " := " + inh + " - " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M81_3")) { // *=赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				codes.add(inh + " := " + inh + " * " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M82_3")) { // /=赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				codes.add(inh + " := " + inh + " / " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M83_3")) { // %=赋值完毕
				Node father = left_node.getFather();
				String inh = father.attribute.get("value");
				String value = father.sons.get(0).attribute.get("value");
				codes.add(inh + " := " + inh + " % " + value);
				father.attribute.put("value", inh);
			} else if (leftest.getName().equals("M26_5")) { // 执行if分支
				Node father = left_node.getFather();
				String b = father.sons.get(0).attribute.get("value");
				codes.add("if " + b + " goto " + (codes.size() + 2));
				father.attribute.put("backpatch", "" + codes.size());
				codes.add(null);
			} else if (leftest.getName().equals("M26_7")) { // if分支执行完毕
				Node father = left_node.getFather();
				int backpatch = Integer.parseInt(father.attribute.get("backpatch"));
				codes.add(null);
				codes.set(backpatch, "goto " + codes.size()); // 回填
				father.attribute.put("backpatch", "" + (codes.size() - 1));
			} else if (leftest.getName().equals("M26_9")) { // 执行else分支
				Node father = left_node.getFather();
				int backpatch = Integer.parseInt(father.attribute.get("backpatch"));
				codes.set(backpatch, "goto " + codes.size()); // 回填
			} else if (leftest.getName().equals("M38_3")) { // 接收while条件
				Node father = left_node.getFather();
				father.attribute.put("backto", "" + codes.size());
			} else if (leftest.getName().equals("M38_7")) { // 执行完while循环体
				Node father = left_node.getFather();
				int backpatch = Integer.parseInt(father.attribute.get("backpatch"));
				int backto = Integer.parseInt(father.attribute.get("backto"));
				codes.add("goto " + backto);
				codes.set(backpatch, "goto " + codes.size()); // 回填
			}

			if (leftest.isTerminal()) {
				if (leftest.getName().equals(input_symbol.getName())) {
					pos++; // 匹配
				} else if (pos < token_list.size() && token_list.get(pos + 1).getName().equals(leftest.getName())) {// error:栈顶的终结符与输入的终结符不匹配
					tbmodel_error.addRow(new String[]{ line + "", 
							"栈顶的终结符\'" + leftest.getName() + "\'与输入的终结符\'"+ input_symbol.getName() + "\'不匹配 ", 
							"跳过输入的终结符\'" + input_symbol.getName() + "\'" });
					pos++; // 跳过一个输入
					stack.push(leftest); // 把栈顶终结符压回
				} else {
					tbmodel_error.addRow(new String[]{ line + "", 
							"栈顶的终结符\'" + leftest.getName() + "\'与输入的终结符\'"+ input_symbol.getName() + "\'不匹配 ", 
							"弹出栈顶终结符\'" + leftest.getName() + "\'" });
				}
			} else {
				List<Production> pros = this.getProductionsByLeft(leftest.getName());
				boolean error = true;
				for (int i = 0; i < pros.size(); i++) {
					// 找到可用来归约的产生式
					if (pros.get(i).getSelect().contains(input_symbol.getName())) {
						String[] right = pros.get(i).getRight();
						tbmodel_parse_result.addRow(new String[]{ pros.get(i).toString() });
						// 将产生式右部反向进栈以备比较
						for (int j = right.length - 1; j >= 0; j--) {
							Symbol temp = getSymbol(right[j]);
							stack.push(temp);
							// 如果右部有非终结符，建立对应结点
							if (!temp.isTerminal()) {
								Node node = new Node(temp.getName(), left_node);
								if (node.getSymbol_name().charAt(0) != 'M') {
									left_node.sons.add(0, node);
								}
								node_stack.push(node);
							}
						}
						error = false;
						break;
					}
				}
				// 产生式错误
				if (error) {
					Production pro = this.getProductionToBlank(leftest.getName());
					if (pro != null) { // 栈顶非终结符能推出空
						tbmodel_error.addRow(new String[]{ line + "", 
								"栈顶非终结符\'" + leftest.getName() + "\'不能接收输入的终结符", 
								"使用能将栈顶非终结符\'" + leftest.getName() + "\'推导为空的产生式，推迟错误处理" });
					} else if (leftest.has("follow", input_symbol.getName())) {
						tbmodel_error.addRow(new String[]{ line + "", 
								"栈顶非终结符\'" + leftest.getName() + "\'不能接收输入的终结符", 
								"跳过输入的终结符\'" + input_symbol.getName() + "\'" });
					} else {
						tbmodel_error.addRow(new String[]{ line + "", 
								"栈顶非终结符\'" + leftest.getName() + "\'不能接收输入的终结符", 
								"跳过栈顶非终结符\'" + leftest.getName() + "\'" });
						pos++;
						stack.push(input_symbol);
					}
				}
			}
		}
		return pro_list;
	}
}
