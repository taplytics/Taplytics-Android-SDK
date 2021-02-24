package test;

import com.licel.stringer.annotations.secured;

public class SomeClass {

    final String NAME = "C#";
    @secured
    final static String LISP = "Lisp";

    @secured
    public String getName() {
        return "Java";
    }
}
