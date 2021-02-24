package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import test.interfaces.HelloMessages;

public class App implements HelloMessages {

    public static void main(String[] args) throws IOException {

        SomeClass world = new SomeClass();
        ResourceBundle rb = ResourceBundle.getBundle("test.messages");
        System.out.println(ENGLISH + " " + world.getName() + " "+rb.getString("worldKey") +"! " + AnotherClass.getStringerHello());
        
    }
}
