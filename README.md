# Hibernate UnProxy

Hibernate UnProxy helper class for unproxing hibernate entities. Support Hibernate 4.2.x, 4.3.x, 5.0.x

## How to get it?

You can use it as a maven dependency:

```xml
<dependency>
    <groupId>org.jmmo</groupId>
    <artifactId>hibernate-unproxy</artifactId>
    <version>1.0</version>
</dependency>
```

Or download the latest build at:
    https://github.com/megaprog/hibernate-unproxy/releases

## How to use it?

```java
MyEntity fullyLoadedEntity = HibernateUnProxy.deepUnProxy(myEntity);
```
    