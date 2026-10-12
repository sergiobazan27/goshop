import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { keycloak } from './app/auth/keycloak';

async function start() {
  const authenticated = await keycloak.init({
    onLoad: 'login-required',
    pkceMethod: 'S256',
    redirectUri: window.location.origin + '/',
  });

  if (authenticated) {
    await bootstrapApplication(App, appConfig).catch((err) => console.error(err));
  }
}

start().catch((error) => {
  console.error('No se pudo iniciar Goshop: ', error);
});
