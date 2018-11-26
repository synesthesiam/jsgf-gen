/**
 * Copyright 1998-2003 Sun Microsystems, Inc.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */

package com.synesthesiam.jsgf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammar;
import edu.cmu.sphinx.jsgf.rule.JSGFRule;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleAlternatives;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleCount;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleName;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleSequence;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleTag;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleToken;

/**
 * Implementation of the parse method(s) on
 * javax.speech.recognition.RuleGrammar.
 */
class RuleParser {

	class ParsedEmptyToken extends ParsedRuleToken {

		public ParsedEmptyToken(int pos) {
			super("EMPTY", pos);
		}
	}

	/* extension of RuleParse with tokenPos interface */
	class ParsedRuleParse extends RuleParse implements TokenPos {

		int pos;

		public ParsedRuleParse(JSGFRuleName rn, JSGFRule r, int pos) throws IllegalArgumentException {
			super(rn, r);
			this.pos = pos;
		}

		@Override
		public int getPos() {
			return pos;
		}
	}

	/* extension of RuleSequence with tokenPos interface */
	class ParsedRuleSequence extends JSGFRuleSequence implements TokenPos {

		int pos;

		public ParsedRuleSequence(List<JSGFRule> rules, int pos) {
			super(rules);
			this.pos = pos;
		}

		@Override
		public int getPos() {
			return pos;
		}

	}

	/* extension of RuleTag with tokenPos interface */
	class ParsedRuleTag extends JSGFRuleTag implements TokenPos {

		int pos;

		public ParsedRuleTag(JSGFRule r, String x, int pos) {
			super(r, x);
			this.pos = pos;
		}

		@Override
		public int getPos() {
			return pos;
		}

	}

	/* extension of RuleToken with tokenPos interface */
	class ParsedRuleToken extends JSGFRuleToken implements TokenPos {

		int pos;

		public ParsedRuleToken(String x, int pos) {
			super(x);
			this.pos = pos;
		}

		@Override
		public int getPos() {
			return pos;
		}

	}

	/*
	 * interface for keeping track of where a token occurs in the tokenized
	 * input string
	 */
	interface TokenPos {
		public int getPos();
	}

	public static List<RuleParse> mparse(String text, JSGFGrammar jsgfGrammar, String ruleName) {
		String inputTokens[] = tokenize(text);
		return mparse(inputTokens, jsgfGrammar, ruleName);
	}

	public static List<RuleParse> mparse(String inputTokens[], JSGFGrammar jsgfGrammar, String ruleName) {
		RuleParser rp = new RuleParser();
		rp.jsgfGrammar = jsgfGrammar;
		List<RuleParse> res = new ArrayList<RuleParse>();
		JSGFRuleGrammar grammar = jsgfGrammar.getRuleGrammar();
		Set<String> rNames = ruleName == null ? grammar.getRuleNames() : new HashSet<>(Arrays.asList(ruleName));
		for (String rName : rNames) {
			if (ruleName == null && !grammar.isEnabled(rName)) {
				continue;
			}
			JSGFRule startRule = grammar.getRule(rName);
			if (startRule == null) {
				throw new IllegalStateException("BAD RULENAME " + rName);
			}
			List<TokenPos> p = rp.parse(grammar, startRule, inputTokens, 0);
			if (p != null && !p.isEmpty()) {
				for (TokenPos tp : p) {
					if (tp.getPos() == inputTokens.length) {
						res.add(new RuleParse(new JSGFRuleName(rName), (JSGFRule) tp));
					}
				}
			}
		}
		return res.isEmpty() ? null : res;
	}

	/*
	 * parse a text string against a particular rule from a particular grammar
	 * returning a RuleParse data structure is successful and null otherwise
	 */
	public static RuleParse parse(String text, JSGFGrammar jsgfGrammar, String ruleName) {
		String inputTokens[] = tokenize(text);
		return parse(inputTokens, jsgfGrammar, ruleName);
	}

	public static RuleParse parse(String inputTokens[], JSGFGrammar jsgfGrammar, String ruleName) {
		List<RuleParse> list = mparse(inputTokens, jsgfGrammar, ruleName);
		if (list != null) {
			for (RuleParse rp : list) {
				if (jsgfGrammar.getRuleGrammar().isRulePublic(rp.getRuleReference().getRuleName())) {
					return rp;
				}
			}
		}
		return null;
	}

	/*
	 * tokenize a string
	 */
	static String[] tokenize(String text) {
		StringTokenizer st = new StringTokenizer(text);
		int size = st.countTokens();
		String res[] = new String[size];
		int i = 0;
		while (st.hasMoreTokens()) {
			res[i++] = st.nextToken().toLowerCase();
		}
		return res;
	}

	private JSGFGrammar jsgfGrammar;

	/*
	 * Parse routine called recursively while traversing the Rule structure in a
	 * depth first manner. Returns a list of valid parses.
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRule r, String[] input, int pos) {

		// System.out.println("PARSE " + r.getClass().getName() + ' ' + pos +
		// ' ' + r);

		if (r instanceof JSGFRuleName) {
			return parse(grammar, (JSGFRuleName) r, input, pos);
		}
		if (r instanceof JSGFRuleToken) {
			return parse(grammar, (JSGFRuleToken) r, input, pos);
		}
		if (r instanceof JSGFRuleCount) {
			return parse(grammar, (JSGFRuleCount) r, input, pos);
		}
		if (r instanceof JSGFRuleTag) {
			return parse(grammar, (JSGFRuleTag) r, input, pos);
		}
		if (r instanceof JSGFRuleSequence) {
			return parse(grammar, (JSGFRuleSequence) r, input, pos);
		}
		if (r instanceof JSGFRuleAlternatives) {
			return parse(grammar, (JSGFRuleAlternatives) r, input, pos);
		} else {
			throw new IllegalStateException("ERROR UNKNOWN OBJECT " + r);
		}
	}

	/*
	 * ALTERNATIVES
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleAlternatives ra, String[] input, int pos) {
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (JSGFRule rule : ra.getRules()) {
			List<TokenPos> p = parse(grammar, rule, input, pos);
			if (p != null) {
				res.addAll(p);
			}
		}
		return res;
	}

	/*
	 * RULECOUNT (e.g. [], *, or + )
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleCount rc, String[] input, int pos) {
		int rcount = rc.getCount();
		ParsedEmptyToken empty = new ParsedEmptyToken(pos);
		List<TokenPos> p = parse(grammar, rc.getRule(), input, pos);
		if (p == null) {
			if (rcount == JSGFRuleCount.ONCE_OR_MORE) {
				return null;
			}
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(empty);
			return res;
		}
		if (rcount != JSGFRuleCount.ONCE_OR_MORE) {
			p.add(empty);
		}
		if (rcount == JSGFRuleCount.OPTIONAL) {
			return p;
		}
		for (int m = 2; m <= input.length - pos; m++) {
			JSGFRule[] ar = new JSGFRule[m];
			Arrays.fill(ar, rc.getRule());
			List<TokenPos> q = parse(grammar, new JSGFRuleSequence(Arrays.asList(ar)), input, pos);
			if (q == null) {
				return p;
			}
			p.addAll(q);
		}
		return p;
	}

	/*
	 * RULE REFERENCES
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleName rn, String[] input, int pos) {
		if (rn.getFullGrammarName() == null) {
			rn.setRuleName(grammar.getName() + '.' + rn.getSimpleRuleName());
		}
		String simpleName = rn.getSimpleRuleName();
		if (simpleName.equals("VOID")) {
			return null;
		}
		if (simpleName.equals("NULL")) {
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleParse(rn, JSGFRuleName.NULL, pos));
			return res;
		}
		JSGFRule ruleref = grammar.getRule(simpleName);
		if (rn.getFullGrammarName() != grammar.getName()) {
			ruleref = null;
		}
		if (ruleref == null) {
			String gname = rn.getFullGrammarName();
			// System.out.println("gname=" + gname);
			if (gname != null && !gname.isEmpty()) {
				JSGFRuleGrammar RG1 = jsgfGrammar.getGrammarManager().retrieveGrammar(gname);
				if (RG1 != null) {
					// System.out.println("simpleName=" + simpleName);
					ruleref = RG1.getRule(simpleName);
					// System.out.println("ruleRef=" + ruleref);
					grammar = RG1;
				} else {
					throw new IllegalStateException("ERROR: UNKNOWN GRAMMAR " + gname);
				}
			}
			if (ruleref == null) {
				throw new IllegalStateException("ERROR: UNKNOWN RULE NAME " + rn.getRuleName() + ' ' + rn);
			}
		}
		List<TokenPos> p = parse(grammar, ruleref, input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (TokenPos tp : p) {
			if (tp instanceof ParsedEmptyToken) {
				res.add(tp);
				continue;
			}
			res.add(new ParsedRuleParse(rn, (JSGFRule) tp, tp.getPos()));
		}
		return res;
	}

	/*
	 * RULESEQUENCE
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleSequence rs, String[] input, int pos) {
		List<JSGFRule> rarry = rs.getRules();
		if (rarry == null || rarry.size() == 0) {
			return null;
		}
		List<TokenPos> p = parse(grammar, rarry.get(0), input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		// System.out.println("seq sz" + p.size());
		for (TokenPos tp : p) {
			// System.out.println("seq " + p.get(j));
			int nPos = tp.getPos();
			if (rarry.size() == 1) {
				if (tp instanceof ParsedEmptyToken) {
					res.add(tp);
					continue;
				}
				res.add(new ParsedRuleSequence(Arrays.asList((JSGFRule) tp), tp.getPos()));
				continue;
			}
			JSGFRule[] nra = Arrays.copyOfRange(rarry.toArray(new JSGFRule[rarry.size()]), 1, rarry.size());
			JSGFRuleSequence nrs = new JSGFRuleSequence(Arrays.asList(nra));
			// System.out.println("2parse " + nPos + nrs);
			List<TokenPos> q = parse(grammar, nrs, input, nPos);
			if (q == null) {
				continue;
			}
			// System.out.println("2 seq sz " + p.size());
			for (TokenPos tp1 : q) {
				// System.out.println("2 seq " + q.get(k));
				// System.out.println("tp " + tp);
				// System.out.println("tp1 " + tp1);
				if (tp1 instanceof ParsedEmptyToken) {
					res.add(tp);
					continue;
				}
				if (tp instanceof ParsedEmptyToken) {
					res.add(tp1);
					continue;
				}
				List<JSGFRule> ra;
				if (tp1 instanceof JSGFRuleSequence) {
					JSGFRuleSequence r2 = (JSGFRuleSequence) tp1;
					List<JSGFRule> r2r = r2.getRules();
					ra = new ArrayList<>(r2r.size() + 1);
					ra.add((JSGFRule) tp);
					ra.addAll(r2r);
				} else {
					ra = Arrays.asList(new JSGFRule[] { (JSGFRule) tp, (JSGFRule) tp1 });
				}
				res.add(new ParsedRuleSequence(ra, tp1.getPos()));
			}
		}
		return res;
	}

	/*
	 * TAGS
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleTag rtag, String[] input, int pos) {
		String theTag = rtag.getTag();
		// System.out.println("tag="+theTag);
		List<TokenPos> p = parse(grammar, rtag.getRule(), input, pos);
		if (p == null) {
			return null;
		}
		List<TokenPos> res = new ArrayList<TokenPos>();
		for (TokenPos tp : p) {
			if (tp instanceof ParsedEmptyToken) {
				res.add(tp);
				continue;
			}
            
			res.add(new ParsedRuleTag((JSGFRule) tp, theTag, tp.getPos()));
		}
		return res;
	}

	/*
	 * LITERAL TOKENS
	 */
	private List<TokenPos> parse(JSGFRuleGrammar grammar, JSGFRuleToken rt, String[] input, int pos) {
		if (pos >= input.length) {
			return null;
		}
		// System.out.println(rt.getText() + " ?= " + input[pos]);
		// TODO: what about case sensitivity ??????
		String tText = rt.getText().toLowerCase();
		if (tText.equals(input[pos]) || (input[pos].equals("%")) || (input[pos].equals("*"))) {
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleToken(rt.getText(), pos + 1));
			if (input[pos].equals("*")) {
				res.add(new ParsedRuleToken(rt.getText(), pos));
			}
			return res;
		} else {
			if (tText.indexOf(' ') < 0) {
				return null;
			}
			if (!tText.startsWith(input[pos])) {
				return null;
			}
			String ta[] = tokenize(tText);
			int j = 0;
			while (true) {
				if (j >= ta.length) {
					break;
				}
				if (pos >= input.length) {
					return null;
				}
				if (!ta[j].equals(input[pos])) {
					return null;
				}
				pos++;
				j++;
			}
			List<TokenPos> res = new ArrayList<TokenPos>();
			res.add(new ParsedRuleToken(rt.getText(), pos));
			return res;
		}
	}
}
