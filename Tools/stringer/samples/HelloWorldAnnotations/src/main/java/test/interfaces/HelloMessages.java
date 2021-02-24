package test.interfaces;

import com.licel.stringer.annotations.secured;

public interface HelloMessages {

    @secured
    public final static String ENGLISH = "Hello";
    public final static String SPANISH = "Hola";
}
