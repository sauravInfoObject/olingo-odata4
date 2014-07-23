/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.olingo.fit.proxy.v4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

//CHECKSTYLE:OFF (Maven checkstyle)
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.olingo.client.api.v4.EdmEnabledODataClient;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.ext.proxy.AbstractService;
import org.apache.olingo.ext.proxy.api.EdmStreamValue;
import org.apache.olingo.ext.proxy.api.PrimitiveCollection;
import org.apache.olingo.fit.proxy.v3.staticservice.microsoft.test.odata.services.astoriadefaultservice.DefaultContainer;
import org.apache.olingo.fit.proxy.v3.staticservice.microsoft.test.odata.services.astoriadefaultservice.types.ContactDetailsCollection;
import org.apache.olingo.fit.proxy.v3.staticservice.microsoft.test.odata.services.astoriadefaultservice.types.PhoneCollection;
import org.apache.olingo.fit.proxy.v4.demo.odatademo.DemoService;
import org.apache.olingo.fit.proxy.v4.demo.odatademo.types.PersonDetail;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.InMemoryEntities;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.AccessLevel;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Address;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Color;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Customer;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Order;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.PersonCollection;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Product;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.ProductDetail;
import org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.ProductDetailCollection;
import org.junit.Test;
//CHECKSTYLE:ON (Maven checkstyle)

public class APIBasicDesignTestITCase extends AbstractTestITCase {

  protected AbstractService<EdmEnabledODataClient> getService() {
    return service;
  }

  protected InMemoryEntities getContainer() {
    return container;
  }

  @Test
  public void readAndCheckForPrimitive() {
    final Customer customer = getContainer().getCustomers().getByKey(1);
    assertNotNull(customer);
    assertNull(customer.getPersonID());

    assertEquals(1, customer.load().getPersonID(), 0);
    service.getContext().detachAll();
  }

  @Test
  public void readAndCheckForComplex() {
    Customer customer = container.getCustomers().getByKey(1); // no http request
    Address homeAddress = customer.getHomeAddress();
    assertNotNull(homeAddress);
    assertNull(homeAddress.getCity());
    assertNull(homeAddress.getPostalCode());
    assertNull(homeAddress.getStreet());

    homeAddress.load(); // HTTP request at complex loading
    assertEquals("London", homeAddress.getCity());
    assertEquals("98052", homeAddress.getPostalCode());
    assertEquals("1 Microsoft Way", homeAddress.getStreet());

    getService().getContext().detachAll();

    homeAddress = container.getCustomers().getByKey(1).load().getHomeAddress(); // HTTP request at entity loading
    assertEquals("London", homeAddress.getCity());
    assertEquals("98052", homeAddress.getPostalCode());
    assertEquals("1 Microsoft Way", homeAddress.getStreet());

    getService().getContext().detachAll();

    customer = container.getOrders().getByKey(8).getCustomerForOrder();
    homeAddress = customer.getHomeAddress().select("City", "PostalCode").expand("SomethingElse"); // no HTTP request
    assertNotNull(homeAddress);
    assertNull(homeAddress.getCity());
    assertNull(homeAddress.getPostalCode());
    assertNull(homeAddress.getStreet());

    try {
      homeAddress.load();
      fail();
    } catch (Exception e) {
      // Generated URL 
      // "<serviceroot>/Orders(8)/CustomerForOrder/HomeAddress?$select=City,PostalCode&$expand=SomethingElse"
      // curently unsupported by test service server
      homeAddress.clearQueryOptions();
    }
  }

  @Test
  public void readWholeEntitySet() {
    PersonCollection person = getContainer().getPeople().execute();
    assertEquals(5, person.size(), 0);

    int pageCount = 1;
    while (person.hasNextPage()) {
      pageCount++;
      assertFalse(person.nextPage().execute().isEmpty());
    }

    assertEquals(2, pageCount);
    service.getContext().detachAll(); // avoid influences
  }

  @Test
  public void loadWithSelect() {
    org.apache.olingo.fit.proxy.v4.staticservice.microsoft.test.odata.services.odatawcfservice.types.Order order =
            getContainer().getOrders().getByKey(8);
    assertNull(order.getOrderID());
    assertNull(order.getOrderDate());

    order.select("OrderID");
    order.load();

    assertNull(order.getOrderDate());
    assertNotNull(order.getOrderID());

    order.clearQueryOptions();
    order.load();
    assertNotNull(order.getOrderDate());
    assertNotNull(order.getOrderID());
    service.getContext().detachAll(); // avoid influences
  }

  @Test
  public void loadWithSelectAndExpand() {
    final Customer customer = getContainer().getCustomers().getByKey(1);

    customer.expand("Orders");
    customer.select("Orders", "PersonID");

    customer.load();
    assertEquals(1, customer.getOrders().size());
    service.getContext().detachAll(); // avoid influences
  }

  @Test
  public void navigateLinks() {
    final Customer customer = getContainer().getCustomers().getByKey(1); // No HTTP Request
    assertNotNull(customer.getCompany().load().getCompanyID()); // singleton: single request
    assertEquals(1, customer.getOrders().execute().size()); // collection: single request

    final Order order = getContainer().getOrders().getByKey(8); // No HTTP Requests
    assertNotNull(order.getCustomerForOrder().load().getPersonID()); // entity type: single request

    service.getContext().detachAll(); // avoid influences
  }

  @Test
  public void createDelete() {
    // Create order ....
    final Order order = getContainer().newEntityInstance(Order.class);
    order.setOrderID(1105);

    final Calendar orderDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    orderDate.clear();
    orderDate.set(2011, 3, 4, 16, 3, 57);
    order.setOrderDate(new Timestamp(orderDate.getTimeInMillis()));

    order.setShelfLife(BigDecimal.ZERO);

    final PrimitiveCollection<BigDecimal> osl = getContainer().newPrimitiveCollection(BigDecimal.class);
    osl.add(BigDecimal.TEN.negate());
    osl.add(BigDecimal.TEN);

    order.setOrderShelfLifes(osl);

    getContainer().getOrders().add(order);
    getContainer().flush();

    Order actual = getContainer().getOrders().getByKey(1105);
    assertNull(actual.getOrderID());

    actual.load();
    assertEquals(1105, actual.getOrderID(), 0);
    assertEquals(orderDate.getTimeInMillis(), actual.getOrderDate().getTime());
    assertEquals(BigDecimal.ZERO, actual.getShelfLife());
    assertEquals(2, actual.getOrderShelfLifes().size());

    service.getContext().detachAll();

    // (1) Delete by key (see EntityCreateTestITCase)
    getContainer().getOrders().delete(1105);
    assertNull(getContainer().getOrders().getByKey(1105));

    service.getContext().detachAll(); // detach to show the second delete case

    // (2) Delete by object (see EntityCreateTestITCase)
    getContainer().getOrders().delete(getContainer().getOrders().getByKey(1105));
    assertNull(getContainer().getOrders().getByKey(1105));

    // (3) Delete by invoking delete method on the object itself
    service.getContext().detachAll(); // detach to show the third delete case
    getContainer().getOrders().getByKey(1105).delete();
    assertNull(getContainer().getOrders().getByKey(1105));

    getContainer().flush();

    service.getContext().detachAll();
    try {
      getContainer().getOrders().getByKey(105).load();
      fail();
    } catch (IllegalArgumentException e) {
    }
    service.getContext().detachAll(); // avoid influences
  }

  @Test
  public void updateComplexProperty() {
    Address homeAddress = container.getCustomers().getByKey(1).getHomeAddress();
    homeAddress.setCity("Pescara");
    homeAddress.setPostalCode("98052");
    container.flush();

    homeAddress = container.getCustomers().getByKey(1).getHomeAddress().load();
    assertEquals("Pescara", homeAddress.getCity());
    assertEquals("98052", homeAddress.getPostalCode());
  }

  @Test
  public void updateAndReadEdmStreamProperty() throws IOException {
    // ---------------------------------------
    // Instantiate Demo Service
    // ---------------------------------------
    final org.apache.olingo.fit.proxy.v4.demo.Service<EdmEnabledODataClient> dservice =
            org.apache.olingo.fit.proxy.v4.demo.Service.getV4(testDemoServiceRootURL);
    dservice.getClient().getConfiguration().setDefaultBatchAcceptFormat(ContentType.APPLICATION_OCTET_STREAM);
    final DemoService dcontainer = dservice.getEntityContainer(DemoService.class);
    assertNotNull(dcontainer);
    dservice.getContext().detachAll();
    // ---------------------------------------
    final String random = RandomStringUtils.random(124, "abcdefghijklmnopqrstuvwxyz");

    final PersonDetail personDetail = dcontainer.getPersonDetails().getByKey(1); // NO HTTP Request

    // 1 HTTP Request to add an Edm.Stream property value about MediaEditLink Photo
    personDetail.setPhoto(dcontainer.newEdmStreamValue("application/octet-stream", IOUtils.toInputStream(random)));

    dcontainer.flush();

    final EdmStreamValue actual = dcontainer.getPersonDetails().getByKey(1).getPhoto().load(); // 1 HTTP Request
    assertEquals(random, IOUtils.toString(actual.getStream()));

    dservice.getContext().detachAll(); // avoid influences
  }

  @Test
  public void getProductDetails() {
    Product product = getContainer().newEntityInstance(Product.class);
    product.setProductID(1012);
    product.setName("Latte");
    product.setQuantityPerUnit("100g Bag");
    product.setUnitPrice(3.24f);
    product.setQuantityInStock(100);
    product.setDiscontinued(false);
    product.setUserAccess(AccessLevel.Execute);
    product.setSkinColor(Color.Blue);

    final PrimitiveCollection<Color> cc = getContainer().newPrimitiveCollection(Color.class);
    cc.add(Color.Red);
    cc.add(Color.Green);

    product.setCoverColors(cc);

    final ProductDetail detail = getContainer().newEntityInstance(ProductDetail.class);
    detail.setProductID(product.getProductID());
    detail.setProductDetailID(1012);
    detail.setProductName("LatteHQ");
    detail.setDescription("High-Quality Milk");

    final ProductDetailCollection detailCollection = getContainer().newEntityCollection(ProductDetailCollection.class);
    detailCollection.add(detail);

    product.setDetails(detailCollection);

    getContainer().getProducts().add(product);
    getContainer().flush(); // The first HTTP Request to create product and the linked product detail

    // the second HTTP Request to execute getProductDetails() operation.
    final ProductDetailCollection result = container.getProducts().getByKey(1012).operations().getProductDetails(1);
    assertEquals(1, result.size());
  }

  @Test
  public void workingWithComplexCollections() throws IOException {
    // ---------------------------------------
    // Instantiate V3 Service (Currently, complex collections not provided by V4 service)
    // ---------------------------------------
    org.apache.olingo.fit.proxy.v3.staticservice.Service<org.apache.olingo.client.api.v3.EdmEnabledODataClient> v3serv =
            org.apache.olingo.fit.proxy.v3.staticservice.Service.getV3(
                    "http://localhost:9080/stub/StaticService/V30/Static.svc");
    v3serv.getClient().getConfiguration().setDefaultBatchAcceptFormat(ContentType.APPLICATION_OCTET_STREAM);
    final DefaultContainer v3cont = v3serv.getEntityContainer(DefaultContainer.class);
    assertNotNull(v3cont);
    v3serv.getContext().detachAll();
    // ---------------------------------------

    final ContactDetailsCollection bci = v3cont.getCustomer().getByKey(-10).getBackupContactInfo(); // 1 HTTP Request
    assertNotNull(bci);
    assertTrue(bci.isEmpty());

    bci.execute();
    assertFalse(bci.isEmpty());
    assertEquals(9, bci.size());

    final PhoneCollection phoneCollection = bci.iterator().next().getMobilePhoneBag(); // No new HTTP Request
    assertFalse(phoneCollection.isEmpty());

    assertEquals("jlessdhjbgglmofcyßucßqbrfßppgzvygdyssßpehkrdetitmßfddsplccvussrvidmkodchdfzjvfgossbciq",
            bci.iterator().next().getHomePhone().getPhoneNumber()); // No new HTTP Request

    // assertNotNull(v3cont.getOrder().getByKey(8).getCustomer().getBackupContactInfo().execute().iterator().next().
    //        getHomePhone().getPhoneNumber());
    // Not supported by the test service BTW generates a single request as expected: 
    // <service root>/Order(8)/Customer/BackupContactInfo
    v3serv.getContext().detachAll();

    // Static test service doesn't support query options about complexes: the following provides just a client example
    v3cont.getCustomer().getByKey(-10).getBackupContactInfo().
            select("HomePhone").
            filter("HomePhone eq 12345").
            skip(1).
            top(10).
            orderBy("HomePhone").
            execute(); // New HTTP Request

    v3serv.getContext().detachAll();
  }

  @Test
  public void workingWithPrimitiveCollections() throws IOException {
    final PrimitiveCollection<String> emails = container.getPeople().getByKey(1).getEmails();
    assertNotNull(emails);
    assertTrue(emails.isEmpty());
    assertFalse(emails.execute().isEmpty());

    getService().getContext().detachAll();

    // container.getOrders().getByKey(1).getCustomerForOrder().getEmails().execute().isEmpty());
    // Not supported by the test service BTW generates a single request as expected: 
    // <service root>/Orders(1)/CustomerForOrder/Emails
  }

  @Test
  public void workingWithSingletons() {
    assertNotNull(container.getCompany().getVipCustomer().load().getPersonID());
  }
}