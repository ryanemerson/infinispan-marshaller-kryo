package org.infinispan.marshaller.kryo.unit;

import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
public class UserSerializer extends Serializer<User> {

   public static final AtomicInteger writeCount = new AtomicInteger();
   public static final AtomicInteger readCount = new AtomicInteger();

   public void write (Kryo kryo, Output output, User object) {
      writeCount.incrementAndGet();
      output.writeString(object.name);
   }

   public User read (Kryo kryo, Input input, Class<User> type) {
      readCount.incrementAndGet();
      return new User(input.readString());
   }
}