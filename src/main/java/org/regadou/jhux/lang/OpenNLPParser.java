package org.regadou.jhux.lang;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import opennlp.tools.parser.AbstractBottomUpParser;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.util.Span;
import org.regadou.jhux.Expression;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.impl.Operator;
import org.regadou.jhux.ref.ExpressionImpl;
import org.regadou.jhux.ref.Word;

public class OpenNLPParser implements org.regadou.jhux.Parser {
   
   private static final String DEFAULT_SENTENCE_MODEL = "/en-sent.bin";
   private static final String DEFAULT_TOKENIZER_MODEL = "/en-token.bin";
   private static final String DEFAULT_PARSER_MODEL = "/en-parser-chunking.bin";
   private Parser parser;
   private Tokenizer tokenizer;
   private SentenceDetector sdetector;
   private Map dictionary;
   private boolean loadingMessage;

   public OpenNLPParser() {
      this(new TreeMap(), false, false);
   }

   public OpenNLPParser(Map dictionary) {
      this(dictionary, false, false);
   }

   public OpenNLPParser(Map dictionary, boolean lazyLoading, boolean loadingMessage) {
      this.dictionary = dictionary;
      this.loadingMessage = loadingMessage;
      if (!lazyLoading) {
         checkDependencies();
      }
   }

   public Expression parse(String txt) {
      List<Sentence> sentences = getSentences(txt);
      switch (sentences.size()) {
         case 0:
            return new ExpressionImpl();
         case 1:
            return new ExpressionImpl(sentences.get(0));
         default:
            List params = new ArrayList();
            for (Sentence s : sentences) {
               params.add(new ExpressionImpl(s));
            }
            return new ExpressionImpl((Function) null, params);
      }
   }

   public List<Sentence> getSentences(String text) {
      checkDependencies();
      List<Sentence> sentences = new ArrayList<>();
      for (String sentence : sdetector.sentDetect(text)) {
         sentences.add(new Sentence(sentence, tokenizer, parser, dictionary));
      }
      return sentences;
   }

   private void checkDependencies() {
      if (sdetector == null || tokenizer == null || parser == null) {
         if (loadingMessage) {
            System.out.println("Loading parser data for OpenNLP ...");
         }
         getSentenceDetector();
         getTokenizer();
         getParser();
      }
   }

   private SentenceDetector getSentenceDetector() {
      if (sdetector == null) {
         String modelFile = DEFAULT_SENTENCE_MODEL;
         InputStream modelInput = getClass().getResourceAsStream(modelFile);
         if (modelInput == null) {
            throw new RuntimeException("Model file " + modelFile + " was not found");
         }
         try {
            SentenceModel sentenceModel = new SentenceModel(modelInput);
            modelInput.close();
            sdetector = new SentenceDetectorME(sentenceModel);
         } catch (IOException e) {
            throw new RuntimeException(e);
         } finally {
            try {
               modelInput.close();
            } catch (final IOException e) {
            }
         }
      }
      return sdetector;
   }

   private Tokenizer getTokenizer() {
      if (tokenizer == null) {
         String modelFile = DEFAULT_TOKENIZER_MODEL;
         //TODO: should use a url loader service i.e Context.url(String url).getInputStream();
         InputStream modelInput = getClass().getResourceAsStream(modelFile);
         if (modelInput == null) {
            throw new RuntimeException("Model file " + modelFile + " was not found");
         }
         try {
            TokenizerModel tokenModel = new TokenizerModel(modelInput);
            modelInput.close();
            tokenizer = new TokenizerME(tokenModel);
         } catch (IOException e) {
            throw new RuntimeException(e);
         } finally {
            try {
               modelInput.close();
            } catch (final IOException e) {
            }
         }
      }
      return tokenizer;
   }

   private opennlp.tools.parser.Parser getParser() {
      if (parser == null) {
         String modelFile = DEFAULT_PARSER_MODEL;
         //TODO: should use a url loader service i.e Context.url(String url).getInputStream();
         InputStream modelInput = getClass().getResourceAsStream(modelFile);
         if (modelInput == null) {
            throw new RuntimeException("Model file " + modelFile + " was not found");
         }
         try {
            ParserModel model = new ParserModel(modelInput);
            parser = ParserFactory.create(model);
         } catch (IOException e) {
            throw new RuntimeException(e);
         } finally {
            try {
               modelInput.close();
            } catch (IOException e) {
            }
         }
      }
      return parser;
   }
   
}

class Sentence implements Expression {

   private String text;
   private Function function;
   private List parameters = new ArrayList();
   private StringBuilder debugInfo;

   public Sentence(String text, Tokenizer tokenizer, Parser parser, Map dictionary) {
      this.text = (text == null) ? "" : text;
      this.debugInfo = new StringBuilder();
      for (Reference token : getTokens(tokenizer, parser, dictionary))
         parameters.add(token);
   }

   @Override
   public Object getValue() {
      Function f = (function == null) ? Operator.NOOP : function;
      return f.execute(parameters.toArray());
   }
    
   @Override
   public Function getFunction() {
      return function;
   }

   @Override
   public Object[] getParameters() {
      return parameters.toArray();
   }

   @Override
   public String toString() {
      return text;
   }

   public String getDebugInfo() {
      return debugInfo.toString();
   }

   private Reference[] getTokens(Tokenizer tokenizer, Parser parser, Map dictionary) {
      if (text == null || text.trim().isEmpty())
         return new Reference[0];
      Span[] spans = tokenizer.tokenizePos(text);
      Parse p = getParse(spans, parser);
      getDebugInfo(debugInfo, p, "");
      return getTokens(p, dictionary);
   }

   private Parse getParse(Span[] spans, Parser parser) {
      Parse p = new Parse(text, new Span(0, text.length()), AbstractBottomUpParser.INC_NODE, 1, 0);
      for (int s = 0; s < spans.length; s++) {
         Span span = spans[s];
         p.insert(new Parse(text, span, AbstractBottomUpParser.TOK_NODE, 0, s));
      }
      return parser.parse(p);
   }

   private void getDebugInfo(StringBuilder buffer, Parse parse, String prompt) {
      String type = parse.getType();
      String label;
      if (type.equals("TK"))
         label = parse.getCoveredText();
      else {
         PartOfSpeech pos = PartOfSpeech.getInstance(type);
         if (pos == null)
            label = "{"+type+"}";
         else {
            Word.Type wt = pos.getType();
            String name = (wt == null) ? type : wt.name().toLowerCase();
            label = "<"+name+":"+pos.getName()+">";
         }
      }
      buffer.append(prompt).append(label).append("\n");
      if (parse.getChildCount() > 0) {
         prompt += " ";
         for (Parse child : parse.getChildren())
            getDebugInfo(buffer, child, prompt);
      }
   }

   private Reference[] getTokens(Parse p, Map dictionary) {
      String type = p.getType();
      PartOfSpeech pos = PartOfSpeech.getInstance(type);
      if (pos != null) {
         Word.Type wt = pos.getType();
         if (wt != null)
            return getWord(p.getCoveredText(), wt, dictionary);
      }
      else if (type.length() == 1 && isPunctuation(type.charAt(0)))
         return getWord(type, Word.Type.getInstance("punctuation"), dictionary);
      int nc = p.getChildCount();
      switch (nc) {
         case 0:
            return new Reference[0];
         case 1:
            return getTokens(p.getChildren()[0], dictionary);
         default:
            Reference[] tokens = new Reference[nc];
            Parse[] children = p.getChildren();
            for (int t = 0; t < nc; t++) {
               Reference[] subtokens = getTokens(children[t], dictionary);
               switch (subtokens.length) {
                  case 0:
                     break;
                  case 1:
                     tokens[t] = subtokens[0];
                     break;
                  default:
                     tokens[t] = new ExpressionImpl((Object[])subtokens);
               }
            }
            return tokens;
      }
   }

   private boolean isPunctuation(char c) {
      return ",;:.?!".indexOf(c) >= 0;
   }
   
   private Reference[] getWord(String txt, Word.Type type, Map dictionary) {
      return new Reference[]{new Word(txt, (type == null) ? Word.Type.WORD : type, JHUX.unref(dictionary.get(txt)), null)};
   }
}

class PartOfSpeech {

   private static final Map<String,PartOfSpeech> INSTANCES = new HashMap<>();
   private static final String[][] DATA = {
      {"CC", "Coordinating conjunction"},
      {"CD", "Cardinal number", "adjective"},
      {"DT", "Determiner"},
      {"EX", "Existential there", "pronoun"},
      {"FW", "Foreign word"},
      {"IN", "Preposition or subordinating conjunction"},
      {"JJ", "Adjective"},
      {"JJR", "Adjective", "comparative"},
      {"JJS", "Adjective", "superlative"},
      {"LS", "List item marker", "punctuation"},
      {"MD", "Modal", "adverb"},
      {"NN", "Noun", "singular or mass"},
      {"NNS", "Noun", "plural"},
      {"NNP", "Proper noun", "singular"},
      {"NNPS", "Proper noun", "plural"},
      {"PDT", "Predeterminer"},
      {"POS", "Possessive ending"},
      {"PRP", "Personal pronoun"},
      {"PRP$", "Possessive pronoun"},
      {"RB", "Adverb"},
      {"RBR", "Adverb", "comparative"},
      {"RBS", "Adverb", "superlative"},
      {"RP", "Particle", "punctuation"},
      {"SYM", "Symbol", "punctuation"},
      {"TO", "Preposition"},
      {"UH", "Interjection"},
      {"VB", "Verb", "base form"},
      {"VBD", "Verb", "past tense"},
      {"VBG", "Verb", "gerund or present participle"},
      {"VBN", "Verb", "past participle"},
      {"VBP", "Verb", "non­3rd person singular present"},
      {"VBZ", "Verb", "3rd person singular present"},
      {"WDT", "Whathever determiner"},
      {"WP", "Whatever pronoun"},
      {"WP$", "Possessive whatever pronoun"},
      {"WRB", "Whatever adverb"}
   };

   static {
      for (int i = 0; i < DATA.length; i++) {
         String[] data = DATA[i];
         PartOfSpeech pos = new PartOfSpeech(data);
         for (String txt : data)
            INSTANCES.put(txt.toLowerCase(), pos);
      }
   }

   public static PartOfSpeech getInstance(String txt) {
      return INSTANCES.get(txt.toLowerCase());
   }

   private String code;
   private String name;
   private String description;
   private Word.Type type;

   private PartOfSpeech(String[] data) {
      code = data[0];
      name = data[1];
      description = (data.length > 2) ? data[2] : data[1];
      for (String txt : data[1].split(" ")) {
         type = Word.Type.getInstance(txt);
         if (type != null)
            break;
      }
      if (type == null) {
         if (data.length > 2) {
            
         }
         if (type == null)
            type = Word.Type.WORD;
      }
   }

   public String getCode() {
      return code;
   }

   public String getName() {
      return name;
   }

   public String getDescription() {
      return description;
   }

   public Word.Type getType() {
      return type;
   }

   @Override
   public String toString() {
      return "[PartOfSpeech "+name+"]";
   }
}
