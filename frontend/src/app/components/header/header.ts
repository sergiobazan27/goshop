import { Component, input } from '@angular/core';
import { keycloak } from '../../auth/keycloak';

@Component({
  imports: [],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly businessName = input('Café Olivo');
  readonly sectionName = input('Panel de servicio');
  readonly accountName = input('SB');

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
