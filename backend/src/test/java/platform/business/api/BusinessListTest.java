package platform.business.api;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import platform.business.domain.BusinessStatus;
import platform.business.infrastructure.persistence.BusinessEntity;
import platform.business.infrastructure.persistence.BusinessRepository;
import platform.membership.infrastructure.persistence.MembershipRepository;
import platform.membership.infrastructure.persistence.MembershipEntity;
import platform.membership.domain.MembershipRole;

/** HTTP and queries run against disposable PostgreSQL, never the Compose development database. */
@QuarkusTest
@TestProfile(BusinessListTest.Profile.class)
class BusinessListTest {
  @TestHTTPResource URI baseUri;
  @Inject BusinessRepository businesses;
  @Inject MembershipRepository memberships;
  private UUID businessId;
  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void seed() {
    QuarkusTransaction.requiringNew().run(() -> {
      memberships.deleteAll();
      businesses.deleteAll();
      for (String name : new String[] {"Café Olivo", "CAFÉ Norte", "Pan 100%", "Pan 100A", "Mesa_1", "MesaA1"}) {
        var business = new BusinessEntity();
        business.name = name;
        business.status = name.equals("CAFÉ Norte") ? BusinessStatus.SUSPENDED : BusinessStatus.ACTIVE;
        businesses.persist(business);
        if (name.equals("Café Olivo")) businessId = business.id;
      }
    });
  }

  @Test
  void requiresAuthentication() throws Exception {
    assertEquals(401, get("").statusCode());
    assertEquals(401, changeStatus(businessId, "{\"status\":\"SUSPENDED\"}").statusCode());
  }

  @Test
  @TestSecurity(user = "owner", roles = "owner")
  void deniesOperationalUsers() throws Exception {
    assertEquals(403, get("").statusCode());
    assertEquals(403, changeStatus(businessId, "{\"status\":\"SUSPENDED\"}").statusCode());
  }

  @Test
  @TestSecurity(user = "admin-owner", roles = {"admin", "owner"})
  @OidcSecurity(claims = @Claim(key = "sub", value = "owner-sub"))
  void suspensionBlocksOperationsAndReactivationRestoresThemWithoutDeletingData() throws Exception {
    QuarkusTransaction.requiringNew().run(() -> {
      var membership = new MembershipEntity();
      membership.business = businesses.findById(businessId);
      membership.keycloakUserId = "owner-sub";
      membership.role = MembershipRole.OWNER;
      memberships.persist(membership);
    });
    assertEquals(200, getPath("api/me/owner-check").statusCode());
    var suspended = changeStatus(businessId, "{\"status\":\"SUSPENDED\"}");
    assertEquals(200, suspended.statusCode(), suspended.body());
    assertEquals("SUSPENDED", new JsonObject(suspended.body()).getString("status"));
    assertEquals(200, changeStatus(businessId, "{\"status\":\"SUSPENDED\"}").statusCode());
    var blocked = getPath("api/me/owner-check");
    assertEquals(403, blocked.statusCode(), blocked.body());
    assertTrue(blocked.body().contains("Business.Inactive"));
    var profile = getPath("api/me");
    assertEquals(200, profile.statusCode());
    assertEquals("SUSPENDED", new JsonObject(profile.body()).getJsonObject("business").getString("status"));
    var active = changeStatus(businessId, "{\"status\":\"ACTIVE\"}");
    assertEquals(200, active.statusCode(), active.body());
    assertEquals("ACTIVE", new JsonObject(active.body()).getString("status"));
    assertEquals(200, getPath("api/me/owner-check").statusCode());
    QuarkusTransaction.requiringNew().run(() -> {
      assertEquals(6, businesses.count());
      assertEquals(1, memberships.count());
      assertEquals(businessId, memberships.findByKeycloakUserId("owner-sub").orElseThrow().business.id);
    });
  }

  @Test
  @TestSecurity(user = "admin", roles = "admin")
  void validatesStatusRequestsAndMissingBusinesses() throws Exception {
    for (String body : new String[] {"null", "{}", "{\"status\":\"INVALID\"}"}) {
      assertEquals(400, changeStatus(businessId, body).statusCode(), body);
    }
    var missing = changeStatus(UUID.randomUUID(), "{\"status\":\"ACTIVE\"}");
    assertEquals(404, missing.statusCode(), missing.body());
    assertTrue(missing.body().contains("Business.NotFound"));
  }

  private HttpResponse<String> changeStatus(UUID id, String body) throws Exception {
    var request = HttpRequest.newBuilder(baseUri.resolve("api/admin/businesses/" + id + "/status"))
      .header("Content-Type", "application/json")
      .PUT(HttpRequest.BodyPublishers.ofString(body)).build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> getPath(String path) throws Exception {
    return client.send(HttpRequest.newBuilder(baseUri.resolve(path)).GET().build(),
      HttpResponse.BodyHandlers.ofString());
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
