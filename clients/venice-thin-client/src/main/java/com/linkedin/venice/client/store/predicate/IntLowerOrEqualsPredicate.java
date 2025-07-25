package com.linkedin.venice.client.store.predicate;

public class IntLowerOrEqualsPredicate implements IntPredicate {
  private final int threshold;

  IntLowerOrEqualsPredicate(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public boolean evaluate(int value) {
    return value <= threshold;
  }

  @Override
  public String toString() {
    return "IntLowerOrEqualsPredicate{threshold=" + threshold + "}";
  }
}
