package org.regadou.jhux.lang;

import org.regadou.jhux.Parser;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.commons.beanutils.BeanMap;
import org.regadou.jhux.Expression;
import org.regadou.jhux.Function;
import org.regadou.jhux.JHUX;
import org.regadou.jhux.Reference;
import org.regadou.jhux.Type;
import org.regadou.jhux.impl.Complex;
import org.regadou.jhux.impl.Operator;
import org.regadou.jhux.impl.RestFunction;
import org.regadou.jhux.impl.RootType;
import org.regadou.jhux.ref.ExpressionImpl;
import org.regadou.jhux.ref.JavaReference;
import org.regadou.jhux.ref.Property;
import org.regadou.jhux.ref.Resource;
import org.regadou.jhux.ref.Value;
import org.regadou.jhux.sys.Context;

public class JhuxParser implements Parser {

   private static final int MINIMUM_TERMINALS = 3;
   private static final String SYNTAX_SYMBOLS = "()[]{}\",;";
   private static final char SPACE_CHAR = 0x20;
   private static final char FIRST_HI_BLANK = 0x7F;
   private static final char LAST_HI_BLANK = 0xA0;
   private static final char FIRST_ACCENT = 0xC0;
   private static final char LAST_ACCENT = 0x2AF;
   
   private static final Reference NULL_DATA = new Reference() {

      @Override
      public Object getOwner() {
         return null;
      }

      @Override
      public String getKey() {
         return "null";
      }

      @Override
      public Object getValue() {
         return null;
      }
   };
   
   public static class ParserStatus {
      public int pos;
      public char end, end2;
      public char[] chars;
      public Object previousToken;
      public Set<Integer> lines = new TreeSet<>();

      public ParserStatus(String txt) {
         this.chars = (txt == null) ? new char[0] : txt.toCharArray();
      }

      public char nextChar() {
         return (pos+1 >= chars.length) ? '\0' : chars[pos+1];
      }

      public char previousChar() {
         return (pos <= 0) ? '\0' : chars[pos-1];
      }

      public void linecount() {
         if (chars[pos] == '\n')
            lines.add(pos);
      }

      public int lineno() {
         int line = 1;
         Iterator<Integer> i = lines.iterator();
         while (i.hasNext()) {
            line++;
            if (i.next() > pos)
               break;
         }
         return line;
      }
   }
   
      
   private final Map<String,Object> KEYWORDS = new TreeMap<>();

   @Override
   public Expression parse(String txt) {
      return parseExpression(new ParserStatus(txt));
   }

   private boolean isBlank(char c) {
      return c <= SPACE_CHAR || (c >= FIRST_HI_BLANK && c <= LAST_HI_BLANK);
   }

   private boolean isSymbol(char c) {
      if (c == '_')
         return false;
      return (c > ' ' && c < '0') || (c > '9' && c < 'A')
          || (c > 'Z' && c < 'a') || (c > 'z' && c < FIRST_HI_BLANK);
   }

   private boolean isDigit(char c) {
      return (c >= '0' && c <= '9');
   }

   private boolean isAlpha(char c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'
          || (c >= FIRST_ACCENT && c <= LAST_ACCENT);
   }
   
   private Object getToken(char c, ParserStatus status) {
      char next = status.nextChar();
      switch (c) {
         case '"':
            status.pos++;
            status.end = status.end2 = c;
            return parseString(status);
         case '(':
            status.pos++;
            status.end = status.end2 = ')';
            return parseExpression(status);
         case '[':
            status.pos++;
            return parseArray(status);
         case '{':
            status.pos++;
            return parseObject(status);
         case '.':
            if (next == '/' || isAlpha(next))
               return parseName(status);
         case '+':
         case '-':
            return isDigit(next) ? parseNumber(status) : parseSymbol(status);
         case '/':
            if (isBlank(next))
               return parseName(status);
         case ':':
         case '?':
            return isAlpha(next) ? parseName(status) : parseSymbol(status);
         case ')':
         case ']':
         case '}':
            throw new RuntimeException("Line "+status.lineno()+": invalid end of sequence "+c);
         case ',':
            return Operator.JOIN;
         case ';':
            return Operator.EVAL;
         case '#':
            char previous = status.previousChar();
            return (previous == 0 || previous == '\n' || previous == '\r') ? parseComment(status, c) : parseName(status);
         default:
            if (isDigit(c))
               return parseNumber(status);
            else if (isAlpha(c))
               return parseName(status);
            else
               return parseSymbol(status);
      }
   }

   private Expression parseExpression(ParserStatus status) {
      List<Expression> expressions = null;
      Function function = null;
      List params = new ArrayList();
      char end = status.end;
      char end2 = status.end2;
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == end || c == end2)
            break;
         else if (!isBlank(c)) {
            Object token = getToken(c, status);
            if (token != null) {
               status.previousToken = token;
               if (function == null) {
                  function = getFunction(token);
                  if (function != null)
                     continue;
               }
               params.add(token);
            }
            else if (!params.isEmpty() || function != null) {
               if (expressions == null)
                  expressions = new ArrayList<>();
               expressions.add(new ExpressionImpl(function, params.toArray()));
               function = null;
               params.clear();
            }
         }
      }

      if (end > 0 && c != end)
         throw new RuntimeException("Line "+status.lineno()+": syntax error: closing character "+end+" missing");
      if (!params.isEmpty() || function != null) {
         Expression exp = new ExpressionImpl(function, params.toArray());
         if (expressions == null)
            return exp;
         expressions.add(exp);
      }
      if (expressions != null) {
         if (expressions.size() == 1)
            return expressions.get(0);
         function = Operator.EVAL;
         params = expressions;
      }
      return new ExpressionImpl(function, params.toArray());
   }

   private Object parseToken(ParserStatus status) {
      Object elem = null;
      int start = status.pos;
      char end = status.end;
      char end2 = status.end2;
      char c = 0;
      boolean gotBlank = false;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == end || c == end2)
            break;
         else if (isBlank(c))
            gotBlank = true;
         else if (elem != null)
            throw new RuntimeException("Line "+status.lineno()+": syntax error (element already found) after "+new String(status.chars, start, status.pos-start));
         else if (gotBlank)
            throw new RuntimeException("Line "+status.lineno()+": syntax error (space in identifier) after "+new String(status.chars, start, status.pos-start));
         else
            elem = getToken(c, status);
      }

      if (end != 0 && status.pos >= status.chars.length) {
         String endchars = ""+status.end;
         if (status.end != status.end2)
            endchars += status.end2;
         throw new RuntimeException("Line "+status.lineno()+": syntax error (end character "+endchars+" not found) after "+new String(status.chars, start, status.pos-start));
      }
      else
         return elem;
   }

   private Reference parseString(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      int start = status.pos;
      char end = status.end;
      char end2 = status.end2;
      int terminals = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         char c = status.chars[status.pos];
         if (c == end || c == end2) {
            if (terminals == 0 || terminals >= MINIMUM_TERMINALS)
               return new Value(buffer.toString()); //TODO: interpret templating $(...) if terminal was more than 2
            else
               terminals++;
         }
         else if (terminals > 0) {
            while (terminals > 1) {
               buffer.append(end);
               terminals--;
            }
            buffer.append(c);
         }
         else if (c == '\\' && terminals < MINIMUM_TERMINALS) {
            status.pos++;
            if (status.pos >= status.chars.length)
               break;
            c = status.chars[status.pos];
            switch (c) {
               case 'b':
                  buffer.append('\b');
                  break;
               case 'f':
                  buffer.append('\f');
                  break;
               case 'n':
                  buffer.append('\n');
                  break;
               case 'r':
                  buffer.append('\r');
                  break;
               case 't':
                  buffer.append('\t');
                  break;
               case 'x':
                  try {
                     int ascii = Integer.parseInt(new String(status.chars,status.pos+1, 2), 16);
                     buffer.append((char)ascii);
                     status.pos += 2;
                  } catch (Exception e) {
                     throw new RuntimeException("Line "+status.lineno()+": invalid ascii escape: "+e.getMessage());
                  }
                  break;
               case 'u':
                  try {
                     int ascii = Integer.parseInt(new String(status.chars,status.pos+1, 4), 16);
                     buffer.append((char)ascii);
                     status.pos += 4;
                  } catch (Exception e) {
                     throw new RuntimeException("Line "+status.lineno()+": invalid unicode escape: "+e.getMessage());
                  }
                  break;
               case '"':
               case '\'':
               case '\\':
                  buffer.append(c);
                  break;
                default:
                  throw new RuntimeException("Line "+status.lineno()+": invalid escape \\"+c);
            }
         }
         else if (c < 0x20 && buffer.length() == 0) {
            terminals++;
            buffer.append(c);
         }
         else
            buffer.append(c);
      }

      throw new RuntimeException("Line "+status.lineno()+": end of string not found after "+new String(status.chars, start, status.pos-start));
   }

   private Object parseNumber(ParserStatus status) {
      StringBuilder buffer = new StringBuilder();
      boolean end=false, digit=false, hexa=false, decimal=false, exponent=false,
              complex=false, time=false, sign=false, prob=false;

      for (; status.pos < status.chars.length; status.pos++) {
         char c = status.chars[status.pos];
         if (c == status.end || c == status.end2) {
            status.pos--;
            break;
         }
         switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
               digit = sign = true;
               break;
            case '-':
               if (time)
                  break;
               else if (digit && !sign && !decimal && !hexa && !exponent && !complex) {
                  time = true;
                  break;
               }
            case '+':
               if (digit || sign)
                  end = true;
               else
                  sign = true;
               break;
            case '.':
               if (decimal || exponent || hexa)
                  end = true;
               else if (!time)
                  decimal = true;
               break;
            case ':':
               if (time)
                  break;
               else if (!digit || decimal || hexa || exponent || complex)
                  end = true;
               else
                  time = true;
               break;
            case '%':
               if (digit && !hexa && !decimal && !exponent && !complex && !time) {
                  prob = true;
                  buffer.append(c);
                  status.pos++;
               }
               end = true;
               break;
            case 'e':
            case 'E':
               if (hexa)
                  break;
               else if (!digit || exponent)
                  end = true;
               else {
                  exponent = decimal = true;
                  digit = sign = false;
               }
               break;
            case 'a':
            case 'A':
            case 'b':
            case 'B':
            case 'c':
            case 'C':
            case 'd':
            case 'D':
            case 'f':
            case 'F':
               if (!hexa)
                  end = true;
               break;
            case 'x':
            case 'X':
               if (hexa || decimal || exponent || complex || time)
                  end = true;
               else if (buffer.toString().equals("0"))
                  hexa = true;
               else
                  end = true;
               break;
            case 'i':
            case 'I':
               if (hexa || complex || time)
                  end = true;
               else {
                  complex = true;
                  decimal = exponent = digit = sign = false;
               }
               break;
            default:
               end = true;
         }
         if (end) {
            status.pos--;
            break;
         }
         else
            buffer.append(c);
      }

      String txt = buffer.toString();
      if (!digit)
         return null;
      if (prob)
         return Float.parseFloat(txt.substring(0, txt.length()-1))/100;
      if (hexa)
         return Integer.parseInt(txt.substring(2), 16);
      if (complex)
         return new Complex(txt);
      if (decimal || exponent)
         return new Double(txt);
      if (time)
         return JHUX.get(Context.class).convert(txt, Date.class);
      return new Long(txt);
   }

   private List parseArray(ParserStatus status) {
      List lst = new ArrayList();
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == ']')
            break;
         else if (!isBlank(c)) {
            status.end = ',';
            status.end2 = ']';
            Object token = parseToken(status);
            if (token != null)
               lst.add(token);
            c = status.chars[status.pos];
            if (c == ']')
               break;
            else if (token == null)
               throw new RuntimeException("Line "+status.lineno()+": no token between commas");
         }
      }

      if (c != ']')
         throw new RuntimeException("Line "+status.lineno()+": missing end of array ]");
      return lst;
   }

   private Object parseObject(ParserStatus status) {
      Map map = new LinkedHashMap();
      String key = null;
      char c = 0;

      for (; status.pos < status.chars.length; status.pos++) {
         c = status.chars[status.pos];
         if (c == '}')
            break;
         else if (!isBlank(c)) {
            status.end = (key == null) ? ':' : ',';
            status.end2 = '}';
            Object token = parseToken(status);
            if (token == null)
               ;
            else if (key == null) {
               if (token instanceof String)
                  key = token.toString();
               else if (token instanceof Map.Entry)
                  key = ((Map.Entry)token).getKey().toString();
               else
                  throw new RuntimeException("Line "+status.lineno()+": "+token+" is not a valid object key");
            } else {
               map.put(key, token);
               key = null;
            }
            c = status.chars[status.pos];
            if (c == '}')
               break;
            else if (token == null)
               throw new RuntimeException("Line "+status.lineno()+": no token in colon-comma sequence");
         }
      }

      if (key != null)
         throw new RuntimeException("Line "+status.lineno()+": key "+key+" does not have a value");
      else if (c != '}')
         throw new RuntimeException("Line "+status.lineno()+": missing end of object }");
      Object obj = map;
      Object type = map.get("class");
      if (type != null) {
         try {
            Class klass = Class.forName(type.toString());
            BeanMap bean = new BeanMap(klass.newInstance());
            Context cx = JHUX.get(Context.class);
            for (Object k : map.keySet()) {
               String name = String.valueOf(k);
               if (!bean.containsKey(name))
                  continue;
               Class t = bean.getType(name);
               Object value = map.get(k);
               if (!t.isAssignableFrom((value == null) ? Void.class : value.getClass()))
                  value = cx.convert(value, t);
               bean.put(name, value);
            }
            obj = bean.getBean();
         }
         catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {}
      }
      return obj;
   }

   private Object parseName(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      char next;
      boolean uri = false, java = false;
      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (c == status.end || c == status.end2) {
            status.pos--;
            break;
         }
         else if (uri) {
            if (isBlank(c))
               break;
            continue;
         }
         else if (java) {
            next = status.nextChar();
            if (isAlpha(next) || isDigit(next) || next == '.')
               continue;
            break;
         }
         switch (c) {
            case ':':
               next = status.nextChar();
               if (!isAlpha(next) && next != '/') {
                  c = ' ';
                  break;
               }
            case '/':
               uri = true;
               continue;
            case '.':
               next = status.nextChar();
               if (length == 0 && (next == '.' || next == '/')) {
                  uri = true;
                  continue;                  
               }
               if (isAlpha(next)) {
                  if (Package.getPackage(new String(status.chars, start, length)) != null)
                     java = true;
                  else
                     uri = true;
                  continue;
               }
               break;
         }
         if (isBlank(c) || isSymbol(c)) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);
      try { 
         if (java)
            return new JavaReference(txt);
         if (uri) 
            return new Resource(new URI(txt));
         Object value = getKeyword(txt);
         if (value != null)
            return value;
         return new Property(txt);
      }
      catch (Exception e) { throw new RuntimeException("Line "+status.lineno()+": "+e, e); }
   }

   private Object parseSymbol(ParserStatus status) {
      int start = status.pos;
      int length = 0;
      for (; status.pos < status.chars.length; status.pos++, length++) {
         char c = status.chars[status.pos];
         if (SYNTAX_SYMBOLS.indexOf(c) >= 0) {
            if (start != status.pos)
               status.pos--;
            else
               length++;
            break;
         }
         else if (isBlank(c) || !isSymbol(c) || c == status.end || c == status.end2) {
            status.pos--;
            break;
         }
      }

      String txt = new String(status.chars, start, length);
      Object data = getKeyword(txt);
      if (data != null)
         return data;
      throw new RuntimeException("Line "+status.lineno()+": unknown symbol "+txt);
   }
   
   private Object parseComment(ParserStatus status, char end) {
      int sequence = 0;
      while (status.pos < status.chars.length && status.chars[status.pos] == end) {
         status.linecount();
         status.pos++;
         sequence++;
      }

      for (; status.pos < status.chars.length; status.pos++) {
         status.linecount();
         char c = status.chars[status.pos];
         if (sequence == 1) {
            if (c == '\n' || c == '\r') {
               status.pos--;
               break;
            }
         }
         else if (c == end) {
            int got = 0;
            while (status.pos < status.chars.length && status.chars[status.pos] == end) {
               status.linecount();
               status.pos++;
               got++;
            }
            if (got >= sequence) {
               status.pos--;
               break;
            }
         }
      }
      return null;
   }
   
   private Function getFunction(Object token) {
      if (token instanceof Function)
         return (Function)token;
      if (token instanceof Expression)
         return null;
      if (token instanceof Reference)
         return getFunction(((Reference)token).getValue());
      return null;
   }
   
   private Object getKeyword(String txt) {
      if (KEYWORDS.isEmpty()) {
         for (Function f : RestFunction.values())
            KEYWORDS.put(f.getName(), f);
         for (Operator f : Operator.values()) {
            KEYWORDS.put(f.getName(), f);
            String symbol = f.getSymbol();
            if (symbol != null)
               KEYWORDS.put(symbol, f);
         }
         for (Type t : RootType.values())
            KEYWORDS.put(t.getName(), t);
         KEYWORDS.put("true", true);
         KEYWORDS.put("false", false);
         KEYWORDS.put("null", NULL_DATA);
      }
      return KEYWORDS.get(txt.toLowerCase());
   }
}
