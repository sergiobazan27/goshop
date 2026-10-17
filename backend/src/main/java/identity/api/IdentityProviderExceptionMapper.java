package identity.api;

import org.jboss.logging.Logger;
import identity.infrastructure.keycloak.IdentityProviderException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import shared.api.ApiError;

@Provider
public class IdentityProviderExceptionMapper implements ExceptionMapper<IdentityProviderException> {
  private static final Logger LOG = Logger.getLogger(IdentityProviderExceptionMapper.class);

  @Override
  public Response toResponse(IdentityProviderException exception) {
    LOG.error("Falló la consulta al proveedor de identidad", exception);
    return Response.status(Response.Status.SERVICE_UNAVAILABLE)
      .type(MediaType.APPLICATION_JSON)
      .entity(new ApiError("Identity.Unavailable", "No se pudo verificar al dueño. Intenta nuevamente."))
      .build();
  }
}
