package org.regadou.jhux.ref;

import org.regadou.jhux.Reference;

public class Word implements Reference {
   
   public static enum Type {
      DETERMINER, ADJECTIVE, ADVERB,
      NOUN, PRONOUN, NAME,
      VERB, PREPOSITION, CONJUNCTION, PUNCTUATION,
      INTERJECTION, ONOMATOPEIA, WORD;
      
      public static Type getInstance(String txt) {
         try { return valueOf(txt.toUpperCase()); }
         catch (Exception e) { return null; }
      }
   }

   private String key;
   private Type type;
   private Object value;
   private Object language;

   public Word() {}

   public Word(String text) {
      this(text, null, null, null);
   }

   public Word(String text, Type type) {
      this(text, type, null, null);
   }

   public Word(String text, Object value) {
      this(text, null, value, null);
   }

   public Word(String text, Type type, Object value) {
      this(text, type, value, null);
   }

   public Word(String text, Object value, Object language) {
      this(text, null, value, language);
   }

   public Word(String text, Type type, Object value, Object language) {
      if ((this.key = text.trim()) == null)
         throw new RuntimeException("Word text is empty");
      this.type = type;
      this.value = value;
      this.language = language;
   }

   @Override
   public Object getOwner() {
      return language;
   }

   @Override
   public String getKey() {
      return key;
   }

   @Override
   public Object getValue() {
      return value;
   }

   public Type getType() {
      return type;
   }

   @Override
   public String toString() {
      return key;
   } 

   @Override
   public boolean equals(Object that) {
      return that != null && toString().equals(that.toString());
   }

   @Override
   public int hashCode() {
      return toString().hashCode();
   }
}
