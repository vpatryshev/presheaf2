package com.presheaf;

import java.util.Date;

public class Sample {
  static Object testme(int i, String x, Date y) {
    return i > 0 ? x : y;
  }
  
  public static void main(String[] args) {
    System.out.println(testme(args.length, "good", new Date()));
  }
}
