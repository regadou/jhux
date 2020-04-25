package org.regadou.jhux;

@FunctionalInterface
public interface Parser {

   Expression parse(String text);
}

