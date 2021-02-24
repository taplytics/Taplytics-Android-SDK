package test;

import test.enums.World;


public class SomeClass implements Cloneable {
    private static String SOME_WORLD = "SOME_WORLD";
    private static AnotherClass ac = new AnotherClass();
    private byte[] key;

    public SomeClass(){
        SOME_WORLD = "OTRA PALABRA";
    }

    public SomeClass(byte[] b){
       this.key = ((byte[]) b.clone());
    }
    
    public String getName() {
        return World.Java.getName();
    }
    
    public Object clone(){
  try{
  SomeClass cloned = (SomeClass)super.clone();
  return cloned;
  }
  catch(CloneNotSupportedException e){
  System.out.println(e);
  return null;
  }
  }
    
}
