package com.synesthesiam.jsgf;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import edu.cmu.sphinx.jsgf.JSGFGrammar;
import edu.cmu.sphinx.jsgf.JSGFGrammarParseException;
import edu.cmu.sphinx.jsgf.JSGFRuleGrammar;
import edu.cmu.sphinx.jsgf.rule.JSGFRule;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleAlternatives;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleCount;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleName;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleSequence;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleTag;
import edu.cmu.sphinx.jsgf.rule.JSGFRuleToken;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.grammar.Grammar;
import edu.cmu.sphinx.linguist.language.grammar.GrammarArc;
import edu.cmu.sphinx.linguist.language.grammar.GrammarNode;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;

// --------------------------------------------------------------------------

public class JsgfGen {
  private static final Logger logger = Logger.getLogger(JsgfGen.class.getName());
  private static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static Random randomizer = new Random();
  private static JSGFRuleGrammar ruleGrammar = null;

  public static void main(String[] args) throws Exception {
    Options options =
        new Options()
        .addOption("h", "help", false, "Print this help message")
        .addOption(Option.builder()
                   .longOpt("grammar")
                   .desc("JSGF grammar")
                   .hasArg()
                   .required()
                   .build())
        .addOption(Option.builder()
                   .longOpt("count")
                   .desc("Number of random sentences to generate")
                   .hasArg()
                   .build())
        .addOption(Option.builder()
                   .longOpt("tags")
                   .desc("Add tags using Markdown entity style")
                   .build())
        .addOption(Option.builder()
                   .longOpt("classes")
                   .desc("Add tags names in upper case")
                   .build())
        .addOption(Option.builder()
                   .longOpt("seed")
                   .desc("Random number generator seed value (defaults to random)")
                   .hasArg()
                   .build())
        .addOption(Option.builder()
                   .longOpt("tokens")
                   .desc("Print all tokens (words) present in the grammar")
                   .build())
        .addOption(Option.builder()
                   .longOpt("replace")
                   .hasArg()
                   .desc("Replace rule alternatives with basic tokens (JSON input)")
                   .build())
        .addOption(Option.builder()
                   .longOpt("exhaustive")
                   .desc("Enumerate all phrases in the grammar")
                   .build())
        .addOption(Option.builder()
                   .longOpt("debug")
                   .desc("Enable finer level of logging to the console")
                   .build());

    // Parse command-line options
    DefaultParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.err.println(e);
      new HelpFormatter().printHelp("jsgf-dump", options);
      System.exit(1);
    }

    if (cmd.hasOption("debug")) {
      // Adjust logging level
      logger.setLevel(Level.FINE);
      logger.getParent().getHandlers()[0].setLevel(Level.FINE);
    }

    if (!cmd.hasOption("count")
        && !cmd.hasOption("tokens")
        && !cmd.hasOption("exhaustive")
        && !cmd.hasOption("replace")) {
      System.err.println("Either --count or --exhaustive or --tokens or --replace is required");
      System.exit(1);
    }

    if (cmd.hasOption("seed")) {
      // Use provided seed
      randomizer = new Random(Integer.parseInt(cmd.getOptionValue("seed")));
    }

    // ------------------------------------------------------------------------

    File grammarFile = new File(cmd.getOptionValue("grammar")).getAbsoluteFile();

    try {
      JSGFGrammar grammar = new JSGFGrammar(
          grammarFile.getParentFile().toURI().toURL(),
          FilenameUtils.removeExtension(grammarFile.getName()),
          false, /* showGrammar */
          true,  /* optimizeGrammar */
          false, /* addSilenceWords */
          false, /* addFillerWords */
          new FakeDictionary());

      grammar.allocate();

      // ------------------------------------------------------------------------

      // Post-processing of sentences
      Function<String, String> postProcess = Function.identity();

      boolean addTags = cmd.hasOption("tags");
      boolean addClasses = cmd.hasOption("classes");
      if (addTags || addClasses) {
        postProcess = (sentence) -> {
          // Parse generated sentence and use JSGF tags
          RuleParse parse = RuleParser.parse(sentence, grammar, null);
          if (parse != null) {
            if (addTags) {
              return makeTaggedSentence(
                  parse.getParse(),
                  info -> String.format("[%s](%s) ", info.taggedText, info.tagName));

            } else if (addClasses) {
              return makeTaggedSentence(parse.getParse(),
                                        info -> info.tagName.toUpperCase() + " ");
            }
          } else {
            logger.warning(String.format("Failed to parse sentence: %s", sentence));
          }

          return sentence;
        };
      }


      if (cmd.hasOption("tokens")) {
        // Print all tokens (words) in the grammar
        Set<String> tokens = new HashSet<>();
        collectTokens(grammar.getInitialNode(), tokens);
        for (String token : tokens) {
          System.out.println(token);
        }
      } else if (cmd.hasOption("count")) {
        // Generate random sentences from grammar
        int numSentences = Integer.parseInt(cmd.getOptionValue("count"));
        Set<String> usedSentences = new HashSet<>();

        for (int i = 0; i < numSentences; i++) {
          String sentence = postProcess.apply(getRandomSentence(grammar));
          if (!usedSentences.contains(sentence)) {
            usedSentences.add(sentence);
            System.out.println(sentence);
          }
        }
      } else if (cmd.hasOption("replace")) {
        // Replace rule alternatives with static tokens
        Gson gson = new Gson();
        Type mapListToken = new TypeToken<Map<String, List<String>>>(){}.getType();

        try (FileReader reader = new FileReader(cmd.getOptionValue("replace"))) {

          Map<String, List<String>> ruleStrings =
              (Map<String, List<String>>)gson.fromJson(reader, mapListToken);

          replaceRules(grammar, ruleStrings);
          grammar.commitChanges();

          // Write to stdout
          ruleGrammar = grammar.getRuleGrammar();
          System.out.println(writeGrammar(ruleGrammar));
        }

      } else if (cmd.hasOption("exhaustive")) {
        // Enumerate all phrases in the grammar
        GrammarNode node = grammar.getInitialNode();
        printSuccessors(node, postProcess);
      }

    } catch (IOException ex) {
      if (ex.getCause() instanceof JSGFGrammarParseException) {
        JSGFGrammarParseException jsgfEx = (JSGFGrammarParseException)ex.getCause();
        System.err.println(String.format("Error at line %s, char %s", jsgfEx.lineNumber, jsgfEx.charNumber));
        System.err.println(jsgfEx.message);
        System.err.println(jsgfEx.details);
        System.exit(1);
      } else {
        throw ex;
      }
    }

  }  // method main

  // --------------------------------------------------------------------------

  private static class TagInfo {
    public String tagName;
    public String taggedText;
  }

  private static String makeTaggedSentence(JSGFRule component,
                                           Function<TagInfo, String> tagHandler) {
    StringBuilder sb = new StringBuilder();
    makeTaggedSentence(component, tagHandler, sb, true);
    return sb.toString().trim();
  }

  private static void makeTaggedSentence(JSGFRule component,
                                         Function<TagInfo, String> tagHandler,
                                         StringBuilder sb,
                                         boolean includeTags) {

    if (component instanceof JSGFRuleTag) {
      final JSGFRuleTag tag = (JSGFRuleTag) component;
      final String tagName = tag.getTag();
      final StringBuilder tagSB = new StringBuilder();
      makeTaggedSentence(tag.getRule(), tagHandler, tagSB, false);

      if (includeTags) {
        TagInfo info = new TagInfo();
        info.tagName = tagName;
        info.taggedText = tagSB.toString().trim();
        sb.append(tagHandler.apply(info));
      } else {
        sb.append(tagSB.toString().trim());
      }
    } else if (component instanceof JSGFRuleAlternatives) {
      final JSGFRuleAlternatives alternatives = (JSGFRuleAlternatives) component;
      for (JSGFRule rule : alternatives.getRules()) {
        makeTaggedSentence(rule, tagHandler, sb, includeTags);
      }
    } else if (component instanceof JSGFRuleCount) {
      final JSGFRuleCount count = (JSGFRuleCount) component;
      final JSGFRule actComponent = count.getRule();
      makeTaggedSentence(actComponent, tagHandler, sb, includeTags);
    } else if (component instanceof RuleParse) {
      final RuleParse parse = (RuleParse) component;
      final JSGFRule actComponent = parse.getParse();
      makeTaggedSentence(actComponent, tagHandler, sb, includeTags);
    } else if (component instanceof JSGFRuleSequence) {
      final JSGFRuleSequence sequence = (JSGFRuleSequence) component;
      for (JSGFRule rule : sequence.getRules()) {
        makeTaggedSentence(rule, tagHandler, sb, includeTags);
      }
    } else if (component instanceof JSGFRuleToken) {
      final JSGFRuleToken token = (JSGFRuleToken)component;
      sb.append(token.getText()).append(' ');
    }
  }

  // --------------------------------------------------------------------------

  private static String getRandomSentence(Grammar grammar) {
    StringBuilder sb = new StringBuilder();
    GrammarNode node = grammar.getInitialNode();

    List<GrammarNode> nodeStack = new ArrayList<>();

    while (!node.isFinalNode()) {
      nodeStack.add(node);

      if (!node.isEmpty()) {
        Word word = node.getWord();
        if (!word.isFiller())
          sb.append(word.getSpelling()).append(' ');
      }
      node = selectRandomSuccessor(node);
      if (node == null) {
        // Back up to viable node (must have hit a <VOID>)
        node = nodeStack.get(nodeStack.size() - 2);
      }
    }
    return sb.toString().trim();
  }


  private static GrammarNode selectRandomSuccessor(GrammarNode node) {
    GrammarArc[] arcs = node.getSuccessors();

    // select a transition arc with respect to the arc-probabilities (which are log and we don't
    // have a logMath here which makes the implementation a little bit messy
    if (arcs.length > 1) {
      double[] linWeights = new double[arcs.length];
      double linWeightsSum = 0;

      final double EPS = 1E-10;

      for (int i = 0; i < linWeights.length; i++) {
        linWeights[i] = (arcs[0].getProbability() + EPS) / (arcs[i].getProbability() + EPS);
        linWeightsSum += linWeights[i];
      }

      for (int i = 0; i < linWeights.length; i++) {
        linWeights[i] /= linWeightsSum;
      }


      double selIndex = randomizer.nextDouble();
      int index = 0;
      for (int i = 0; selIndex > EPS; i++) {
        index = i;
        selIndex -= linWeights[i];
      }

      return arcs[index].getGrammarNode();

    } else if (arcs.length > 0) {
      return arcs[0].getGrammarNode();
    }

    return null;
  }

  private static void collectTokens(GrammarNode node, Set<String> tokens) {
    if (!node.isEmpty()) {
      Word word = node.getWord();
      if (!word.isFiller()) {
        String text = word.getSpelling();
        if (!text.isEmpty()) {
          tokens.add(text);
        }
      }
    }

    for (GrammarArc arc : Arrays.asList(node.getSuccessors())) {
      collectTokens(arc.getGrammarNode(), tokens);
    }
  }

  // --------------------------------------------------------------------------

  private static void replaceRules(JSGFGrammar grammar,
                                   Map<String, List<String>> ruleStrings) {

    JSGFRuleGrammar ruleGrammar = grammar.getRuleGrammar();

    for (String ruleName : ruleGrammar.getRuleNames()) {
      List<String> strings = ruleStrings.get(ruleName);
      if (strings == null) {
        continue;
      }

      JSGFRule rule = ruleGrammar.getRule(ruleName);
      if (rule instanceof JSGFRuleAlternatives) {
        JSGFRuleAlternatives alts = (JSGFRuleAlternatives)rule;
        alts.setRules(strings.stream()
                      .map(str -> new JSGFRuleSequence(
                          Arrays.stream(str.split("\\s"))
                          .map(word -> new JSGFRuleToken(word))
                          .collect(Collectors.toList())))
                      .collect(Collectors.toList()));

      } else {
        logger.warning(String.format("Rule %s is not an alteratives", ruleName));
      }
    }
  }

  // --------------------------------------------------------------------------

  public static String writeGrammar(JSGFRuleGrammar grammar) {
    StringBuilder sb = new StringBuilder();
    sb.append("#JSGF V1.0;").append(LINE_SEPARATOR);
    sb.append(LINE_SEPARATOR);
    sb.append("grammar ").append(grammar.getName()).append(';').append(LINE_SEPARATOR);
    sb.append(LINE_SEPARATOR);
    // Set of comment keys (The import such comment belongs to).
    for (int i = 0; i < grammar.getImports().size(); i++) {
      String curImport = '<' + grammar.getImports().get(i).getRuleName() + '>';
      sb.append("import ").append(curImport + ';').append(LINE_SEPARATOR);
      sb.append(LINE_SEPARATOR);
    }
    for (String ruleName : grammar.getRuleNames()) {
      JSGFRule rule = grammar.getRule(ruleName);
      if (grammar.isRulePublic(ruleName)) {
        sb.append("public ");
      }
      sb.append('<')
          .append(ruleName)
          .append("> = ")
          .append(writeRule(grammar.getName(), rule))
          .append(';')
          .append(LINE_SEPARATOR);

      sb.append(LINE_SEPARATOR);
    }
    return sb.toString();
  }

  // We have to override all of the toString functionality because the original code always fully
  // qualifies grammar rule names (<X.Y> for grammar X), which causes stack overflow exceptions when
  // parsing the grammar back in (if a rule has the same name as the grammar itself -- something
  // that seems to be assumed throughout pocketsphinx)!
  private static String writeRule(String grammarName, JSGFRule rule) {
    // Only the JSGFRuleName branch differs from the original code (aside from replacing all
    // toString instances with writeRule).
    if (rule instanceof JSGFRuleAlternatives) {
      JSGFRuleAlternatives alts = (JSGFRuleAlternatives)rule;
      List<JSGFRule> rules = alts.getRules();
      List<Float> weights = alts.getWeights();

      if (rules == null || rules.size() == 0) {
        return "<VOID>";
      }

      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < rules.size(); ++i) {
        if (i > 0)
          sb.append(" | ");

        if (weights != null)
          sb.append("/" + weights.get(i) + "/ ");

        JSGFRule r = rules.get(i);
        if (rules.get(i) instanceof JSGFRuleAlternatives)
          sb.append("( ").append(writeRule(grammarName, r)).append(" )");
        else {
          sb.append(writeRule(grammarName, r));
        }
      }
      return sb.toString();
    } else if (rule instanceof JSGFRuleSequence) {
      List<JSGFRule> rules = ((JSGFRuleSequence)rule).getRules();
      if (rules.size() == 0) {
        return "<NULL>";
      }
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i < rules.size(); ++i) {
        if (i > 0)
          sb.append(' ');

        JSGFRule r = rules.get(i);
        if ((r instanceof JSGFRuleAlternatives) || (r instanceof JSGFRuleSequence))
          sb.append("( ").append(writeRule(grammarName, r)).append(" )");
        else {
          sb.append(writeRule(grammarName, r));
        }
      }
      return sb.toString();
    } else if (rule instanceof JSGFRuleCount) {
      JSGFRuleCount rCount = (JSGFRuleCount)rule;
      int count = rCount.getCount();
      rule = rCount.getRule();

      if (count == JSGFRuleCount.OPTIONAL) {
        return '[' + writeRule(grammarName, rule) + ']';
      }
      String str = null;

      if ((rule instanceof JSGFRuleToken) || (rule instanceof JSGFRuleName))
        str = writeRule(grammarName, rule);
      else {
        str = '(' + writeRule(grammarName, rule) + ')';
      }

      if (count == JSGFRuleCount.ZERO_OR_MORE)
        return str + " *";
      if (count == JSGFRuleCount.ONCE_OR_MORE) {
        return str + " +";
      }
      return str + "???";
    } else if (rule instanceof JSGFRuleName) {
      JSGFRuleName ruleName = (JSGFRuleName)rule;

      if (grammarName.equals(ruleName.getSimpleGrammarName())) {
        // Don't fully qualify rule names for the current grammar
        return "<" + ruleName.getSimpleRuleName() + ">";
      }
    }

    return rule.toString();
  }

  // --------------------------------------------------------------------------

  private static void printSuccessors(GrammarNode node,
                                      Function<String, String> postProcess) {

    printSuccessors(node, postProcess, new ArrayList<>(), new HashSet<>());
  }

  private static void printSuccessors(GrammarNode node,
                                      Function<String, String> postProcess,
                                      List<String> words,
                                      Set<String> usedSentences) {
    if (node.isFinalNode()) {
      String sentence = words.stream()
                        .collect(Collectors.joining(" "));

      sentence = postProcess.apply(sentence).trim();

      if (!usedSentences.contains(sentence)) {
        usedSentences.add(sentence);
        System.out.println(sentence);
      }
    } else {
      if (!node.isEmpty()) {
        Word word = node.getWord();
        if (!word.isFiller()) {
          words.add(word.getSpelling());
        }
      }

      for (GrammarArc arc : node.getSuccessors()) {
        printSuccessors(arc.getGrammarNode(), postProcess,
                        new ArrayList<>(words),
                        usedSentences);
      }
    }
  }

  // --------------------------------------------------------------------------

  static class FakeDictionary implements Dictionary {
    @Override
    public Word getWord(String text) {
      if ("<sil>".equals(text)) {
        text = "";  // exclude silence token
      }

      return new Word(text, null, false);
    }

    @Override
    public Word getSentenceStartWord() {
      return null;
    }

    @Override
    public Word getSentenceEndWord() {
      return null;
    }

    @Override
    public Word getSilenceWord() {
      return null;
    }

    @Override
    public void allocate() {
    }

    @Override
    public void deallocate() {
    }

    @Override
    public Word[] getFillerWords() {
      return new Word[] { };
    }

    @Override
    public void newProperties(PropertySheet arg0) throws PropertyException {
    }

  }  // class FakeDictionary

  // --------------------------------------------------------------------------

}  // class Program
