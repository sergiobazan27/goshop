package shared.api;

import org.jboss.logging.Logger;
import org.hibernate.exception.ConstraintViolationException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/** Final safety net; business exceptions have their own mapper. */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Exception> {
  private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);

  @Override
  public Response toResponse(Exception exception) {
    if (exception instanceof WebApplicationException web) {
      return web.getResponse();
    }
    if (exception instanceof UnauthorizedException) {
      return Response.status(401).header("WWW-Authenticate", "Bearer").build();
    }
    if (exception instanceof ForbiddenException) {
      return Response.status(403).build();
    }
    // A unique-key race can surface at commit, after the transactional service returns.
    for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException constraint
          && "23505".equals(constraint.getSQLState())) {
        return Response.status(Response.Status.CONFLICT).type(MediaType.APPLICATION_JSON)
          .entity(new ApiError("Data.Conflict", "Ya existe un registro con esos datos únicos."))
          .build();
      }
    }
    LOG.error("Error inesperado procesando la petición", exception);
    return Response.serverError().type(MediaType.APPLICATION_JSON)
      .entity(new ApiError("Server.Unexpected", "No se pudo completar la operación."))
      .build();
  }
}
