package test.case3;

import com.licel.stringer.annotations.insecure;

public class A {

    private String A = "A";
    private String B = "B";
    @insecure
    private static final String protectedStr = "C";
    C c;

    public A(String s) {
        c = new C(protectedStr.getBytes());
        System.out.println("xxx"+protectedStr);
    }
    
    @insecure
    public static void main(String[] args) {
        A a = new A("zzz");
        System.out.println("zzz"+protectedStr);
    }
    
}
