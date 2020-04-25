package org.regadou.jhux.impl;

public class Complex extends Number {

   private double real;
   private double imaginary;

   public Complex(String txt) {
      int index = txt.toLowerCase().indexOf('i');
      if (index < 0)
         real = Double.parseDouble(txt);
      else if (index == 0)
         imaginary = Double.parseDouble(txt.substring(1));
      else {
         int sign = 1, skip = 0;
         switch (txt.charAt(index-1)) {
            case '-':
               sign = -1;
            case '+':
               if (index == 1)
                  txt = "0"+txt;
               else
                  skip = 1;
         }
         real = Double.parseDouble(txt.substring(0, index-skip));
         imaginary = Double.parseDouble(txt.substring(index+1)) * sign;
      }
   }

   public Complex(Number n) {
      if (n instanceof Complex) {
         Complex c = (Complex)n;
         real = c.real;
         imaginary = c.imaginary;
      }
      else
         real = n.doubleValue();
   }

   public Complex(double real, double imaginary) {
      this.real = real;
      this.imaginary = imaginary;
   }

   @Override
   public String toString() {
      String sign = (imaginary < 0) ? "-" : "+";
      return real+sign+"i"+Math.abs(imaginary);
   }

   @Override
   public int intValue() {
      return (int)real;
   }

   @Override
   public long longValue() {
      return (long)real;
   }

   @Override
   public float floatValue() {
      return (float)real;
   }

   @Override
   public double doubleValue() {
      return real;
   }

   public double realValue() {
      return real;
   }

   public double imaginaryValue() {
      return imaginary;
   }

   public Complex add(Number n) {
      Complex c = toComplex(n);
      double real = this.realValue() + c.realValue();
      double imag = this.imaginaryValue() + c.imaginaryValue();
      return new Complex(real, imag);
   }

   public Complex subtract(Number n) {
      Complex c = toComplex(n);
      double real = this.realValue() - c.realValue();
      double imag = this.imaginaryValue() - c.imaginaryValue();
      return new Complex(real, imag);
   }

   public Complex multiply(Number n) {
      Complex c = toComplex(n);
      double real = this.realValue() * c.realValue() - this.imaginaryValue() * c.imaginaryValue();
      double imag = this.realValue() * c.imaginaryValue() + this.imaginaryValue() * c.realValue();
      return new Complex(real, imag);
   }

   public Complex divide(Number n) {
      return multiply(toComplex(n).reciprocal());
   }

   public Complex modulo(Number n) {
      //TODO: complex modulo
      return null;
   }

   public Complex exponant(Number n) {
      //TODO: complex exponant
      return null;
   }

   public Complex root(Number n) {
      //TODO: complex root
      return null;
   }

   public Complex logarithm(Number n) {
      //TODO: complex logarithm
      return null;
   }

   public Complex scale(double alpha) {
      return new Complex(alpha * realValue(), alpha * imaginaryValue());
   }

   public Complex conjugate() {
      return new Complex(realValue(), -imaginaryValue());
   }

   public Complex reciprocal() {
      double scale = realValue() * realValue() + imaginaryValue() * imaginaryValue();
      return new Complex(realValue() / scale, -imaginaryValue() / scale);
   }

   public Complex exp() {
      return new Complex(Math.exp(realValue()) * Math.cos(imaginaryValue()), Math.exp(realValue()) * Math.sin(imaginaryValue()));
   }

   public Complex sin() {
      return new Complex(Math.sin(realValue()) * Math.cosh(imaginaryValue()), Math.cos(realValue()) * Math.sinh(imaginaryValue()));
   }

   public Complex cos() {
      return new Complex(Math.cos(realValue()) * Math.cosh(imaginaryValue()), -Math.sin(realValue()) * Math.sinh(imaginaryValue()));
   }

   public Complex tan() {
      return sin().divide(cos());
   }

   public double abs() {
      return Math.hypot(realValue(), imaginaryValue());
   }

   public double phase() {
      return Math.atan2(realValue(), imaginaryValue());
   }
   
   private Complex toComplex(Number n) {
      if (n instanceof Complex)
         return (Complex)n;
      if (n == null)
         return new Complex(0, 0);
      return new Complex(n.doubleValue(), 0);
   }
}
