package org.infinispan.marshaller.kryo.unit;

import java.util.List;
import java.util.Map;

/**
 * @author Ryan Emerson
 * @since 9.0
 */
class TestObject {
   int id;
   Map<Integer, String> map;
   List<Integer> list;
   User user;

   public TestObject(){}

   TestObject(int id) {
      this.id = id;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TestObject that = (TestObject) o;

      if (id != that.id) return false;
      if (map != null ? !map.equals(that.map) : that.map != null) return false;
      if (list != null ? !list.equals(that.list) : that.list != null) return false;
      return user != null ? user.equals(that.user) : that.user == null;

   }

   @Override
   public int hashCode() {
      int result = id;
      result = 31 * result + (map != null ? map.hashCode() : 0);
      result = 31 * result + (list != null ? list.hashCode() : 0);
      result = 31 * result + (user != null ? user.hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "TestObject{" +
            "id=" + id +
            ", map=" + map +
            ", list=" + list +
            ", user=" + user +
            '}';
   }
}
