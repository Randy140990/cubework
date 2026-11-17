package com.insidethecube.retromon.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Just enough Molang to play the models' animations: numbers, q.anim_time, + - * /, comparisons, ?:,
 * brackets and the math functions (sin and cos in degrees, as Bedrock has them). Anything it doesn't know
 * counts as 0.
 */
public final class Molang {
	private Molang() {}

	/** A compiled expression: give it the animation time (seconds), get a number. */
	public interface Expr {
		double eval(double t);
	}

	public static Expr constant(double v) {
		return t -> v;
	}

	public static Expr parse(String src) {
		String s = src.trim().toLowerCase(Locale.ROOT);
		if (s.startsWith("return ")) s = s.substring(7);
		while (s.endsWith(";")) s = s.substring(0, s.length() - 1).trim();
		if (s.isEmpty()) return constant(0);
		try {
			Parser p = new Parser(s);
			Expr e = p.ternary();
			return e;
		} catch (RuntimeException ex) {
			return constant(0);
		}
	}

	private static final class Parser {
		private final String s;
		private int i;

		Parser(String s) {
			this.s = s;
		}

		private void skip() {
			while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
		}

		private boolean eat(String tok) {
			skip();
			if (s.startsWith(tok, i)) {
				i += tok.length();
				return true;
			}
			return false;
		}

		Expr ternary() {
			Expr c = or();
			if (eat("?")) {
				Expr a = ternary();
				if (!eat(":")) return a;
				Expr b = ternary();
				return t -> c.eval(t) != 0 ? a.eval(t) : b.eval(t);
			}
			return c;
		}

		Expr or() {
			Expr a = and();
			while (eat("||")) {
				Expr l = a, r = and();
				a = t -> (l.eval(t) != 0 || r.eval(t) != 0) ? 1 : 0;
			}
			return a;
		}

		Expr and() {
			Expr a = compare();
			while (eat("&&")) {
				Expr l = a, r = compare();
				a = t -> (l.eval(t) != 0 && r.eval(t) != 0) ? 1 : 0;
			}
			return a;
		}

		Expr compare() {
			Expr a = sum();
			while (true) {
				Expr l = a;
				if (eat("<=")) {
					Expr r = sum();
					a = t -> l.eval(t) <= r.eval(t) ? 1 : 0;
				} else if (eat(">=")) {
					Expr r = sum();
					a = t -> l.eval(t) >= r.eval(t) ? 1 : 0;
				} else if (eat("==")) {
					Expr r = sum();
					a = t -> l.eval(t) == r.eval(t) ? 1 : 0;
				} else if (eat("!=")) {
					Expr r = sum();
					a = t -> l.eval(t) != r.eval(t) ? 1 : 0;
				} else if (eat("<")) {
					Expr r = sum();
					a = t -> l.eval(t) < r.eval(t) ? 1 : 0;
				} else if (eat(">")) {
					Expr r = sum();
					a = t -> l.eval(t) > r.eval(t) ? 1 : 0;
				} else return a;
			}
		}

		Expr sum() {
			Expr a = product();
			while (true) {
				Expr l = a;
				if (eat("+")) {
					Expr r = product();
					a = t -> l.eval(t) + r.eval(t);
				} else if (eat("-")) {
					Expr r = product();
					a = t -> l.eval(t) - r.eval(t);
				} else return a;
			}
		}

		Expr product() {
			Expr a = unary();
			while (true) {
				Expr l = a;
				if (eat("*")) {
					Expr r = unary();
					a = t -> l.eval(t) * r.eval(t);
				} else if (eat("/")) {
					Expr r = unary();
					a = t -> {
						double d = r.eval(t);
						return d == 0 ? 0 : l.eval(t) / d;
					};
				} else return a;
			}
		}

		Expr unary() {
			if (eat("-")) {
				Expr a = unary();
				return t -> -a.eval(t);
			}
			if (eat("+")) return unary();
			if (eat("!")) {
				Expr a = unary();
				return t -> a.eval(t) == 0 ? 1 : 0;
			}
			return atom();
		}

		Expr atom() {
			skip();
			if (eat("(")) {
				Expr e = ternary();
				eat(")");
				return e;
			}
			int start = i;
			char c = i < s.length() ? s.charAt(i) : 0;
			if (Character.isDigit(c) || c == '.') {
				while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
				double v = Double.parseDouble(s.substring(start, i));
				if (i < s.length() && s.charAt(i) == 'f') i++;
				return constant(v);
			}
			if (Character.isLetter(c) || c == '_') {
				while (i < s.length() && (Character.isLetterOrDigit(s.charAt(i)) || s.charAt(i) == '_' || s.charAt(i) == '.')) i++;
				String name = s.substring(start, i);
				if (eat("(")) {
					List<Expr> args = new ArrayList<>();
					skip();
					if (!eat(")")) {
						do args.add(ternary());
						while (eat(","));
						eat(")");
					}
					return function(name, args);
				}
				return variable(name);
			}
			throw new IllegalStateException("Unexpected '" + c + "' in " + s);
		}

		private static Expr arg(List<Expr> a, int i) {
			return i < a.size() ? a.get(i) : constant(0);
		}

		private static Expr function(String name, List<Expr> a) {
			Expr x = arg(a, 0), y = arg(a, 1), z = arg(a, 2);
			return switch (name.startsWith("math.") ? name.substring(5) : name) {
				case "sin" -> t -> Math.sin(Math.toRadians(x.eval(t)));
				case "cos" -> t -> Math.cos(Math.toRadians(x.eval(t)));
				case "abs" -> t -> Math.abs(x.eval(t));
				case "sqrt" -> t -> Math.sqrt(Math.max(0, x.eval(t)));
				case "pow" -> t -> Math.pow(x.eval(t), y.eval(t));
				case "min" -> t -> Math.min(x.eval(t), y.eval(t));
				case "max" -> t -> Math.max(x.eval(t), y.eval(t));
				case "floor" -> t -> Math.floor(x.eval(t));
				case "ceil" -> t -> Math.ceil(x.eval(t));
				case "round" -> t -> Math.round(x.eval(t));
				case "trunc" -> t -> (long) x.eval(t);
				case "clamp" -> t -> Math.max(y.eval(t), Math.min(z.eval(t), x.eval(t)));
				case "lerp" -> t -> x.eval(t) + (y.eval(t) - x.eval(t)) * z.eval(t);
				case "mod" -> t -> {
					double d = y.eval(t);
					return d == 0 ? 0 : x.eval(t) % d;
				};
				case "exp" -> t -> Math.exp(x.eval(t));
				default -> constant(0);
			};
		}

		private static Expr variable(String name) {
			return switch (name) {
				case "q.anim_time", "query.anim_time", "q.life_time", "query.life_time" -> t -> t;
				case "math.pi" -> constant(Math.PI);
				case "true" -> constant(1);
				default -> constant(0);
			};
		}
	}
}
