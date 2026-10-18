package platform.business.api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import platform.business.domain.BusinessStatus;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.business.infrastructure.persistence.BusinessRepository;

/** HTTP and queries run against disposable PostgreSQL, never the Compose development database. */
@QuarkusTest
@TestProfile(BusinessListTest.Profile.class)
class BusinessListTest {
  @TestHTTPResource URI baseUri;
  @Inject BusinessRepository businesses;
  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void seed() {
    QuarkusTransaction.requiringNew().run(() -> {
      businesses.deleteAll();
      for (String name : new String[] {"Café Olivo", "CAFÉ Norte", "Pan 100%", "Pan 100A", "Mesa_1", "MesaA1"}) {
        var business = new BusinessEntity();
        business.name = name;
        business.status = name.equals("CAFÉ Norte") ? BusinessStatus.SUSPENDED : BusinessStatus.ACTIVE;
        businesses.persist(business);
      }
    });
  }

  @Test
  void requiresAuthentication() throws Exception {
    assertEquals(401, get("").statusCode());
  }

  @Test
  @TestSecurity(user = "owner", roles = "owner")
  void deniesOperationalUsers() throws Exception {
    assertEquals(403, get("").statusCode());
  }

  @Test
  @TestSecurity(user = "admin", roles = "admin")
  void listsAllBusinessesWithStablePagesAndLiteralNameSearch() throws Exception {
    var all = json("");
    assertEquals(6, all.getInteger("total"));
    assertEquals(1, all.getInteger("totalPages"));
    assertEquals(6, all.getJsonArray("items").size());
    var first = json("?size=1&search=" + encode("  cAFÉ  "));
    var second = json("?size=1&page=1&search=" + encode("cAFÉ"));
    assertEquals(2, first.getInteger("total"));
    assertEquals(2, first.getInteger("totalPages"));
    String firstId = first.getJsonArray("items").getJsonObject(0).getString("id");
    String secondId = second.getJsonArray("items").getJsonObject(0).getString("id");
    assertNotEquals(firstId, secondId);
    assertEquals(firstId, json("?size=1&search=" + encode("cAFÉ")).getJsonArray("items").getJsonObject(0).getString("id"));
    var suspended = json("?search=" + encode("café") + "&status=SUSPENDED");
    assertEquals(1, suspended.getInteger("total"));
    assertEquals("CAFÉ Norte", suspended.getJsonArray("items").getJsonObject(0).getString("name"));
    assertEquals(1, json("?search=" + encode("100%")).getInteger("total"));
    assertEquals(1, json("?search=" + encode("Mesa_")).getInteger("total"));
    assertEquals(0, json("?search=" + encode("' OR 1=1 --")).getInteger("total"));
    assertTrue(json("?page=100").getJsonArray("items").isEmpty());
  }

  @Test
  @TestSecurity(user = "admin", roles = "admin")
  void rejectsInvalidQueryParameters() throws Exception {
    for (String query : new String[] {"?page=-1", "?page=1000001", "?size=0", "?size=101", "?status=INVALID", "?search=" + "a".repeat(151)}) {
      assertEquals(400, get(query).statusCode(), query);
    }
  }

  private HttpResponse<String> get(String query) throws Exception {
    var request = HttpRequest.newBuilder(baseUri.resolve("api/admin/businesses" + query)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private JsonObject json(String query) throws Exception {
    var response = get(query);
    assertEquals(200, response.statusCode(), response.body());
    return new JsonObject(response.body());
  }

  private String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  public static class Profile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.ofEntries(
        Map.entry("quarkus.oidc.tenant-enabled", "false"),
        Map.entry("quarkus.keycloak.devservices.enabled", "false"),
        Map.entry("quarkus.keycloak.admin-client.server-url", "http://localhost:1"),
        Map.entry("quarkus.keycloak.admin-client.realm", "goshop"),
        Map.entry("quarkus.datasource.db-kind", "postgresql"),
        Map.entry("quarkus.datasource.devservices.enabled", "true"),
        Map.entry("quarkus.datasource.devservices.reuse", "false"),
        Map.entry("quarkus.datasource.devservices.image-name", "postgres:17-alpine"),
        Map.entry("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
      );
    }
  }
}
