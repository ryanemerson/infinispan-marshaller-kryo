# Kryo Marshaller implementation #
This is an implementation of the Infinispan [Marshaller Interface](https://github.com/infinispan/infinispan/blob/master/commons/src/main/java/org/infinispan/commons/marshall/Marshaller.java),
which enables the Kryo library to be utilised for object Marshalling/Unmarshalling between Infinispan clients and server.

## Usage

### Server with Compatability Mode Enabled
1. `mvn install`
2. Copy `infinispan-marshaller-kryo-1.0.0-SNAPSHOT-bundle.jar` to `modules/system/layers/base/org/infinispan/main`
in your infinispan server directory.
3. Add the above jar as a resource in the `module.xml` file location in the same dir as your jar

    ```
    <resources>
        ...
        <resource-root path="infinispan-marshaller-kryo-1.0.0-SNAPSHOT-bundle.jar"/>
        ...
    </resources>
    ```
    
4. Repeat steps 2 and 3 for jars containing .class files of the classes to be marshalled/unmarshalled
5. You must enable compatiblity mode for all caches that you wish to utilise the kryo marshaller, e.g.

    ```
    <local-cache name="default" start="EAGER">
      <compatibility enabled="true" marshaller="org.infinispan.marshaller.kryo.KryoMarshaller"/>
    </local-cache>
    ```

### Client
Your client must depend on:

```
<dependency>
  <groupId>org.infinispan</groupId>
  <artifactId>infinispan-kryo-marshaller</artifactId>
  <version>1.0.0-SNAPSHOT</version>
</dependency>
```
    
#### Hotrod
You must configure your client to utilise the `KryoMarshaller`, e.g.

```
ConfigurationBuilder builder = new ConfigurationBuilder();
      builder.addServer()
            .host("127.0.0.1")
            .port(11222)
            .socketTimeout(1200000)
      .marshaller(KryoMarshaller.class);
```

#### Rest
When putting objects via Rest, you must first marshall them using the `KryoMarshaller` and then 
send them as type `application/octet-stream`. e.g: 

```
byte[] bytes = cacheFactory.getMarshaller().objectToByteBuffer(value);
EntityEnclosingMethod put = new PutMethod(cacheFactory.getRestUrl() + "/" + key);
put.setRequestEntity(new ByteArrayRequestEntity(bytes, "application/octet-stream"));
```

#### Memcached
It's necessary for the MemcachedClient to be configured to use the KryoTranscoder class 
in order for values to be encoded/decoded correctly.
