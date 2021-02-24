package test;

import com.licel.stringer.annotations.insecure;
import com.licel.stringer.annotations.secured;
import test.interfaces.HelloMessages;

@secured
public class App implements HelloMessages {

    @insecure
    final static String worldStr = " World! ";
    @secured
    static String elmundoStr = "El mundo";

    public static void main(String[] args) {
        SomeClass world = new SomeClass();
        System.out.println(ENGLISH + " " + world.getName() + worldStr + AnotherClass.STRINGER_HELLO);
    }
}
