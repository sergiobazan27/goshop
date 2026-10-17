import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

export const keycloak = new Keycloak({
  ...environment.keycloak,
  url: new URL(environment.keycloak.url, window.location.origin).href,
});
