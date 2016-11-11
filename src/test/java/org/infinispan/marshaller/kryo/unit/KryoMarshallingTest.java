package org.infinispan.marshaller.kryo.unit;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.marshaller.kryo.KryoMarshaller;
import org.infinispan.test.AbstractInfinispanTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
@Test
public class KryoMarshallingTest extends AbstractInfinispanTest {

   private final KryoMarshaller marshaller = new KryoMarshaller();
   private final List<TestObject> testObjects = IntStream.range(1, 2).boxed().map(this::getTestObject).collect(Collectors.toList());
   private ByteArrayOutputStream outputStream;

   @BeforeTest
   public void setup() {
      marshaller.getKryo().register(User.class, new UserSerializer());
      UnmodifiableCollectionsSerializer.registerSerializers(marshaller.getKryo());
   }

   @BeforeMethod(alwaysRun = true)
   public void init() {
      outputStream = new ByteArrayOutputStream(1024);
   }

   @AfterMethod(alwaysRun = true)
   public void reset() throws Exception {
      UserSerializer.readCount.set(0);
      UserSerializer.writeCount.set(0);
      outputStream.close();
   }

   private TestObject getTestObject(int id) {
      TestObject testObject = new TestObject(id);
      Map<Integer, String> map = new HashMap<>();
      map.put(id, "Test" + id);
      testObject.map = map;
      testObject.list = IntStream.range(0, id).boxed().collect(Collectors.toList());
      testObject.user = new User("User" + id);
      return testObject;
   }

   public void testObjectMarshalling() throws Exception {
      List<ByteBuffer> serializedObjects = new ArrayList<>(testObjects.size());

      for (TestObject object : testObjects) {
         serializedObjects.add(marshaller.objectToBuffer(object));
      }

      assert serializedObjects.size() == testObjects.size();
      for (int i = 0; i < testObjects.size(); i++) {
         byte[] bytes = serializedObjects.get(i).getBuf();
         Object testObj = testObjects.get(i);
         Object unmarshalledObj = marshaller.objectFromByteBuffer(bytes);
         assert testObj.equals(unmarshalledObj);
      }
   }

   public void testRegisterSerializersAreUtilised() throws Exception {
      TestObject obj = testObjects.get(0);
      byte[] bytes = marshaller.objectToByteBuffer(obj);
      assert UserSerializer.writeCount.get() == 1;
      assert marshaller.objectFromByteBuffer(bytes).equals(obj);
      assert UserSerializer.readCount.get() == 1;
   }

   public void testImmutableCollections() throws Exception {
      int listSize = 10;
      TestObject obj = testObjects.get(0);
      obj.list = Collections.unmodifiableList(IntStream.range(0, 10).boxed().collect(Collectors.toList()));
      byte[] bytes = marshaller.objectToByteBuffer(obj);
      TestObject unmarshalledObj =  (TestObject) marshaller.objectFromByteBuffer(bytes);
      assert unmarshalledObj.list.size() == listSize;
      for (int i = 0; i < listSize; i++)
         assert unmarshalledObj.list.get(i) == i;
   }
}
