package org.infinispan.marshaller.kryo.integration;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotSame;
import static org.testng.AssertJUnit.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.marshaller.kryo.KryoMarshaller;
import org.infinispan.test.AbstractInfinispanTest;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.spy.memcached.CASValue;

/**
 * Test compatibility between embedded caches and Hot Rod endpoints.
 *
 * @author Galder Zamarreño
 * @author Ryan Emerson
 * @since 9.0
 */
@Test(testName = "marshaller.Kryo.EmbeddedRestMemcachedHotRodTest")
public class KryoEmbeddedRestMemcachedHotRodTest extends AbstractInfinispanTest {

   final static String CACHE_NAME = "memcachedCache";

   CompatibilityCacheFactory<String, Object> cacheFactory;

   @BeforeClass
   protected void setup() throws Exception {
      cacheFactory = new CompatibilityCacheFactory<String, Object>(CACHE_NAME, new KryoMarshaller(), CacheMode.LOCAL).setup();
   }

   @AfterClass
   protected void teardown() {
      CompatibilityCacheFactory.killCacheFactories(cacheFactory);
   }

   public void testMemcachedPutEmbeddedRestHotRodGet() throws Exception {
      final String key = "1";

      // 1. Put with Memcached
      Future<Boolean> f = cacheFactory.getMemcachedClient().set(key, 0, "v1");
      assertTrue(f.get(60, TimeUnit.SECONDS));

      // 2. Get with Embedded
      assertEquals("v1", cacheFactory.getEmbeddedCache().get(key));

      // 3. Get with REST
      HttpMethod get = new GetMethod(cacheFactory.getRestUrl() + "/" + key);
      cacheFactory.getRestClient().executeMethod(get);
      assertEquals(HttpStatus.SC_OK, get.getStatusCode());
      assertEquals("text/plain", get.getResponseHeader("Content-Type").getValue());
      assertEquals("v1", get.getResponseBodyAsString());

      // 4. Get with Hot Rod
      assertEquals("v1", cacheFactory.getHotRodCache().get(key));
   }

   public void testEmbeddedPutMemcachedRestHotRodGet() throws Exception {
      final String key = "2";

      // 1. Put with Embedded
      assertEquals(null, cacheFactory.getEmbeddedCache().put(key, "v1"));

      // 2. Get with Memcached
      assertEquals("v1", cacheFactory.getMemcachedClient().get(key));

      // 3. Get with REST
      HttpMethod get = new GetMethod(cacheFactory.getRestUrl() + "/" + key);
      cacheFactory.getRestClient().executeMethod(get);
      assertEquals(HttpStatus.SC_OK, get.getStatusCode());
      assertEquals("v1", get.getResponseBodyAsString());

      // 4. Get with Hot Rod
      assertEquals("v1", cacheFactory.getHotRodCache().get(key));
   }

   public void testRestPutEmbeddedMemcachedHotRodGet() throws Exception {
      final String key = "3";
      final Object value = "<hey>ho</hey>";

      // 1. Put with REST
      byte[] bytes = cacheFactory.getMarshaller().objectToByteBuffer(value);
      EntityEnclosingMethod put = new PutMethod(cacheFactory.getRestUrl() + "/" + key);
      put.setRequestEntity(new ByteArrayRequestEntity(bytes, "application/octet-stream"));
      HttpClient restClient = cacheFactory.getRestClient();
      restClient.executeMethod(put);
      assertEquals(HttpStatus.SC_OK, put.getStatusCode());
      assertEquals("", put.getResponseBodyAsString().trim());

      // 2. Get with Embedded (given a marshaller, it can unmarshall the result)
      assertEquals(value, cacheFactory.getEmbeddedCache().get(key));

      // 3. Get with Memcached (given a marshaller, it can unmarshall the result)
      assertEquals(value, cacheFactory.getMemcachedClient().get(key));

      // 4. Get with Hot Rod (given a marshaller, it can unmarshall the result)
      assertEquals(value, cacheFactory.getHotRodCache().get(key));
   }

   public void testHotRodPutEmbeddedMemcachedRestGet() throws Exception {
      final String key = "4";

      // 1. Put with Hot Rod
      RemoteCache<String, Object> remote = cacheFactory.getHotRodCache();
      assertEquals(null, remote.withFlags(Flag.FORCE_RETURN_VALUE).put(key, "v1"));

      // 2. Get with Embedded
      assertEquals("v1", cacheFactory.getEmbeddedCache().get(key));

      // 3. Get with Memcached
      assertEquals("v1", cacheFactory.getMemcachedClient().get(key));

      // 4. Get with REST
      HttpMethod get = new GetMethod(cacheFactory.getRestUrl() + "/" + key);
      cacheFactory.getRestClient().executeMethod(get);
      assertEquals(HttpStatus.SC_OK, get.getStatusCode());
      assertEquals("v1", get.getResponseBodyAsString());
   }

   public void testEmbeddedReplaceMemcachedCAS() throws Exception {
      final String key1 = "5";

      // 1. Put with Memcached
      Future<Boolean> f = cacheFactory.getMemcachedClient().set(key1, 0, "v1");
      assertTrue(f.get(60, TimeUnit.SECONDS));
      CASValue oldValue = cacheFactory.getMemcachedClient().gets(key1);

      // 2. Replace with Embedded
      assertTrue(cacheFactory.getEmbeddedCache().replace(key1, "v1", "v2"));

      // 4. Get with Memcached and verify value/CAS
      CASValue newValue = cacheFactory.getMemcachedClient().gets(key1);
      assertEquals("v2", newValue.getValue());
      assertNotSame("The version (CAS) should have changed, " +
            "oldCase=" + oldValue.getCas() + ", newCas=" + newValue.getCas(),
            oldValue.getCas(), newValue.getCas());
   }

   public void testHotRodReplaceMemcachedCAS() throws Exception {
      final String key1 = "6";

      // 1. Put with Memcached
      Future<Boolean> f = cacheFactory.getMemcachedClient().set(key1, 0, "v1");
      assertTrue(f.get(60, TimeUnit.SECONDS));
      CASValue oldValue = cacheFactory.getMemcachedClient().gets(key1);

      // 2. Replace with Hot Rod
      VersionedValue versioned = cacheFactory.getHotRodCache().getVersioned(key1);
      assertTrue(cacheFactory.getHotRodCache().replaceWithVersion(key1, "v2", versioned.getVersion()));

      // 4. Get with Memcached and verify value/CAS
      CASValue newValue = cacheFactory.getMemcachedClient().gets(key1);
      assertEquals("v2", newValue.getValue());
      assertTrue("The version (CAS) should have changed", oldValue.getCas() != newValue.getCas());
   }

   public void testEmbeddedHotRodReplaceMemcachedCAS() throws Exception {
      final String key1 = "7";

      // 1. Put with Memcached
      Future<Boolean> f = cacheFactory.getMemcachedClient().set(key1, 0, "v1");
      assertTrue(f.get(60, TimeUnit.SECONDS));
      CASValue oldValue = cacheFactory.getMemcachedClient().gets(key1);

      // 2. Replace with Hot Rod
      VersionedValue versioned = cacheFactory.getHotRodCache().getVersioned(key1);
      assertTrue(cacheFactory.getHotRodCache().replaceWithVersion(key1, "v2", versioned.getVersion()));

      // 3. Replace with Embedded
      assertTrue(cacheFactory.getEmbeddedCache().replace(key1, "v2", "v3"));

      // 4. Get with Memcached and verify value/CAS
      CASValue newValue = cacheFactory.getMemcachedClient().gets(key1);
      assertEquals("v3", newValue.getValue());
      assertTrue("The version (CAS) should have changed", oldValue.getCas() != newValue.getCas());
   }
}
