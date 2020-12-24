# Deep Instantiator

Deep instantiator is a java class to aid you in instantiating deep data models in Java.

# Why?

In the past, I've had to instantiate minimally populated instances of objects that had layers and layers of nested POJOs - that's annoying to code manually and gets really annoying when you have to change it.

Why deep instantiate a class with no information in it in the first place? Good question. 

If you are trying to test if a system is correctly marshalling/unmarshalling objects, but the library declaring the specification for those objects is shifting over time...you're in a pickle. A quick way I've tried to keep track of this model drift is to write a test that will create an empty POJO (null generics/wrappers, empty collections, etc), marshall it, and compare it against a reference file of the last known spec. This way a test will start failing if you update the declaring library's version and the model changes without you noticing.

Up until putting together the Deep Instantiator, I coded this manually with a very long series of nested builder statements - pretty gross. I couldn't find a definitive code snippet or answer for this kind of issue, so why not make it and share it?

# What does it do?

Deep Instatiator will use reflection to traverse through the class you want to instantiate and instantiate all POJOs inside of it recursively. Maps, Lists, and Sets are supported as well, these will be instantiated as HashMaps, ArrayLists, and HashSets. 

Maps offer a bit of trickiness in reflection as you can come across a map that has a generic/wrapper key (which we leave as null), but a POJO value (which we want to instantiate and recursively burrow down). In this instance, default keys are made with support for String (`""`), Integer (`0`), Long (`0L`), Float (`0F`), and Double (`0D`) keys. This is done in order to be able to create the POJO value in the Map which can be then be traversed without throwing up marshalling issues because of null keys.

A few classes are specifically ignored because they do not play nicely with reflection trying to call their default constructor. They can be customised using the constuctor. By default these ignore classes are:
 - String.class
 - ZonedDateTime.class
 - LocalDateTime.class
 - LocalDate.class

# Usage

It's pretty straightforward to use. All you need to do is create (or instantiate _hurr hurr_) an instance of the `DeepInstantiator`, and then call `instantiate()` passing in the class you would like to traverse and instantiate. That's all!

# Example

Let's take the following POJOs as a simple example - imagine if we had 100 classes instead... :( 

```java
class MyObject {
    private MyNestedObj nestedObj;
}

class MyNestedObject {
    private MyDeeperNestedObject deeperNestedObject;
    private String testString;
    private Map<String, MyObjectInsideAMap> stringObjectMap;
}

class MyDeeperNestedObject {
    private Integer testInteger;
    private List<Double> testDoubleList;
}

class MyObjectInsideAMap {
    private Integer testIntegerInAnObjectInAMap;
}
```

We can instantiate the top level POJO using the Deep Instantiator and it will create empty POJOs and null generics/wrappers for all fields recursively.

```java
DeepInstantiator dI = new DeepInstantiator();
MyObject obj = dI.instantiate(MyObject.class);
```

Marshalling that object into JSON will result in something along the lines of...

```json
{
  "nestedObj" : {
    "deeperNestedObject" : {
      "testInteger" : null,
      "testDoubleList" : []
    },
    "testString" : null,
    "stringObjectMap" : {
      "" : {
        "testIntegerInAnObjectInAMap" : null
      }
    }
  }
}
```

Neat! Now we can compare that marshalled string against a reference to see if the model has changed, without having to hand craft an empty instance of `MyObject`, `MyNestedObject`, etc.

# Licensing

MIT - It's a snippet of java code, go nuts.

