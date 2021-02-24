package test.case1;

import com.licel.stringer.annotations.secured;

public class A {

    private String A = "A";
    private String B = "B";
    @secured
    private String protectedStr = "C";
    C c;

    public A(String s) {
        c = new C(protectedStr.getBytes());
        System.out.println("xxx"+protectedStr);
    }
    
    public static void main(String[] args) {
        A a = new A("zzz");
    }
    
}
