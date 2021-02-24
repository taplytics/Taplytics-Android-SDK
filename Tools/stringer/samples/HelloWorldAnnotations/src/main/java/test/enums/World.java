package test.enums;

import com.licel.stringer.annotations.secured;

@secured
public enum World {

    Java("Java"),
    Android("Android");
    private final String name;

    World(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
