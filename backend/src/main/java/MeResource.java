import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me")
@Authenticated 
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {
    @Inject 
    JsonWebToken token;
    
    @Inject 
    SecurityIdentity identity;

    @GET 
    public MeResponse me() {
        return new MeResponse(
            token.getSubject(), 
            identity.getPrincipal().getName(), 
            identity.getRoles());
    }

    public record MeResponse(
        String sub,
        String username,
        Set<String> roles
    ) {}
}
