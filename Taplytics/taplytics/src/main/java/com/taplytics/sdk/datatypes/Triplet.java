/*
 * Copyright © 2020 Taplytics Inc. See https://taplytics.com/terms/ for more
 */

package com.taplytics.sdk.datatypes;

public class Triplet<T, U, V> {
   public T first;
   public U second;
    public V third;

    public Triplet(T first, U second, V third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    T getFirst() {
        return first;
    }

    U getSecond() {
        return second;
    }

    V getThird() {
        return third;
    }
}