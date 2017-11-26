## Table of Contents

* [1. License](#1-license)
* [2. Overview](#2-overview)
* [3. API](#3-overview)
* [4. Releases](#4-releases)

## 1. License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## 2. Overview

Although *Java* generics are rather limited comparing to 'true' generics, it's still possible to extract and use their data in some circumstances:  

Here we are unable to extract type argument's value (*String*) given, say, a reference to the `data` field:   
```java
class MyClass {
    List data = new ArrayList<String>();
}
```
  
In contrast, here we can find out that target type argument's value is *Number*:  
```java
class MyClass implements Comparable<Number> {
}
```

Current library helps with type argument values extraction and processing.

## 3. API

**Extract type argument value**

That functionality is covered by the [TypeArgumentResolver](src/main/java/tech/harmonysoft/oss/jenome/resolve/TypeArgumentResolver.java) interface.  

*Example*

Given the declarations below:  
```java
class MyInterface<A, B> {}
class StringParent<T> implements MyInterface<String, T> {}
class Child extends StringParent<Long> {}
```

We can find out the following:  
```java
typeArgumentResolver.resolve(MyInterface.class, Child.class, 0) -> String
typeArgumentResolver.resolve(MyInterface.class, Child.class, 1) -> Long
```  

Real-world usage example:  

```java
interface Handler<T> {
    void handle(T input);
}

class StringHandler implements Handler<String> {
    public void handle(String input) {}
}

class LongHandler implements Handler<Long> {
    public void handle(Long input) {}
}

@Component
class Router {
    
    private final Map<Type, Handler<?>> handlers;
    
    @Autowired
    public Router(Collection<Handler<?>> handlers) {
        this.handlers = JenomeResolveUtil.byTypeValue(handlers);
    }
    
    public void process(Object data) {
        Handler handler = handlers.get(data.getClass());
        if (handler == null) {
            throw new IllegalArgumentException(String.format(
                "No handler is registered for payload of type %s. Known payload mappings: %s",
                data.getClass().getSimpleName(), handlers
            ));
         }
         handler.handle(data);
    }
}
```

**Check if one type IS-A another type**

This functionality is covered by the [TypeComplianceMatcher](src/main/java/tech/harmonysoft/oss/jenome/match/TypeComplianceMatcher.java).  

*Example*

```java
interface TestInterface<A, B, C> {}
class TestInterfaceImpl<A, B, C> implements TestInterface<A, B, C> {}
class SimpleBaseClass implements TestInterface<Integer, Long, String> {}
class SimpleMatchedClass extends TestInterfaceImpl<Integer, Long, String> {}
class SimpleUnmatchedClass extends TestInterfaceImpl<Integer, Long, Number> {}

typeComplianceMatcher.match(SimpleBaseClass.class.getGenericInterfaces()[0], SimpleMatchedClass.class.getGenericSuperclass()) -> true
typeComplianceMatcher.match(SimpleBaseClass.class.getGenericInterfaces()[0], SimpleUnmatchedClass.class.getGenericSuperclass()) -> false
```

This is a must-have functionality when we want to, say, enhance autowiring rules by type argument values. E.g. consider a spring context with beans of the following types:  
```java
MyClass<Integer>
MyClass<Long>
MyClass<String>
MyClass<StringBuilder>
``` 

We'd like to be able to autowire arguments like `Collection<Number>` (`MyClass<Integer>` and `MyClass<Long>` go here) and `Collection<CharSequence>` (`MyClass<String>` and `MyClass<StringBuilder>` should be provided).  

*Note: right now Spring checks only the base type (`MyClass`) and provides all such beans regarding the type argument's value.*  

## 4. Releases

[Release Notes](RELEASE.md)