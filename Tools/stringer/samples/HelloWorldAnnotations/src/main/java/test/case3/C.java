package test.case3;

import com.licel.stringer.annotations.insecure;

@insecure
public class C {

	String text = "Text";
    byte[] data;

    public C(byte[] data) {
        this.data = data;
    }
}
