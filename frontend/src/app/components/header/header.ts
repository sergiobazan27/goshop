import { Component } from '@angular/core';
import { keycloak } from '../../auth/keycloak';

@Component({
  imports: [],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  async logout() {
    try {
      await keycloak.logout({
        redirectUri: window.location.origin + '/',
      });
    } catch (error) {
      console.error('No se pudo cerrar sesion: ', error);
    }
  }
}
