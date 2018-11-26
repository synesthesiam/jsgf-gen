package com.synesthesiam.jsgf;

/*
 * File:    $HeadURL: https://svn.sourceforge.net/svnroot/jvoicexml/trunk/src/org/jvoicexml/Application.java$
 * Version: $LastChangedRevision: 63 $
 * Date:    $LastChangedDate $
 * Author:  $LastChangedBy: schnelle $
 *
 * JSAPI - An independent reference implementation of JSR 113.
 *
 * Copyright (C) 2007 JVoiceXML group - http://jvoicexml.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.cmu.sphinx.jsgf.rule.JSGFRule;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleAlternatives;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleCount;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleName;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleSequence;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleTag;

//Comp. 2.0.6

class RuleParse extends JSGFRule {
	private JSGFRuleName ruleReference;

	private JSGFRule parse;

	public RuleParse(JSGFRuleName ruleReference, JSGFRule parse) {
		this.ruleReference = ruleReference;
		this.parse = parse;
	}

	private void addTags(List<String> tags, JSGFRule component) {
		if (component instanceof JSGFRuleTag) {
			final JSGFRuleTag tag = (JSGFRuleTag) component;
			final String tagName = tag.getTag();
			tags.add(tagName);
		} else if (component instanceof JSGFRuleAlternatives) {
			final JSGFRuleAlternatives alternatives = (JSGFRuleAlternatives) component;
			for (JSGFRule rule : alternatives.getRules()) {
				addTags(tags, rule);
			}
		} else if (component instanceof JSGFRuleCount) {
			final JSGFRuleCount count = (JSGFRuleCount) component;
			final JSGFRule actComponent = count.getRule();
			addTags(tags, actComponent);
		} else if (component instanceof RuleParse) {
			final RuleParse parse = (RuleParse) component;
			final JSGFRule actComponent = parse.getParse();
			addTags(tags, actComponent);
		} else if (component instanceof JSGFRuleSequence) {
			final JSGFRuleSequence sequence = (JSGFRuleSequence) component;
			for (JSGFRule rule : sequence.getRules()) {
				addTags(tags, rule);
			}
		}
	}

	public JSGFRule getParse() {
		return parse;
	}

	public JSGFRuleName getRuleReference() {
		return ruleReference;
	}

	public List<String> getTags() {
		if (parse == null) {
			return null;
		}

		final List<String> parseTags = new ArrayList<>();
		addTags(parseTags, parse);
		return Collections.unmodifiableList(parseTags);
	}

	@Override
	public String toString() {
		if (parse == null) {
			return "";
		}

		StringBuffer str = new StringBuffer();

		if (ruleReference != null) {
			str.append(ruleReference);
		}

		str.append(parse.toString());

		if (ruleReference != null) {
			str.append("</ruleref>");
		}

		return str.toString();
	}

}
