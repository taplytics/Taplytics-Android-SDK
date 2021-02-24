package test.enums;

public enum World {

    Java("Java"),
    Android("Android");
    private final String name;

    World(String name) {
        this.name = name;
    }
    
    public String getName(){
        return name;
    }
}
