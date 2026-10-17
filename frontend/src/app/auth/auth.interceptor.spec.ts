import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { CurrentUserService } from '../core/services/current-user.service';
import { authInterceptor } from './auth.interceptor';
import { keycloak } from './keycloak';

describe('Interceptor con API configurable', () => {
  const originalApiUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    vi.spyOn(keycloak, 'updateToken').mockResolvedValue(false);
    keycloak.token = 'test-access-token';
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    vi.restoreAllMocks();
    environment.apiUrl = originalApiUrl;
    keycloak.token = undefined;
  });

  it.each(['https://api.example.test/api', '/api'])(
    'consulta el perfil y autentica solo la API configurada: %s',
    async (apiUrl) => {
      environment.apiUrl = apiUrl;
      const result = firstValueFrom(TestBed.inject(CurrentUserService).getMe());
      await Promise.resolve();
      await Promise.resolve();
      const request = TestBed.inject(HttpTestingController).expectOne(`${apiUrl}/me`);
      expect(request.request.headers.get('Authorization')).toBe('Bearer test-access-token');
      expect(keycloak.updateToken).toHaveBeenCalledWith(30);
      const profile = {
        sub: 'admin-id',
        username: 'admin',
        roles: ['admin'],
        membershipRole: null,
        business: null,
      };
      request.flush(profile);
      expect(await result).toEqual(profile);
    },
  );

  it('no transmite el token a otro origen ni a rutas con prefijos parecidos', () => {
    environment.apiUrl = 'https://api.example.test/api';
    const http = TestBed.inject(HttpClient);
    const controller = TestBed.inject(HttpTestingController);
    for (const url of [
      'https://other.example.test/api/me',
      'https://api.example.test/api-other/me',
    ]) {
      http.get(url).subscribe();
      const request = controller.expectOne(url);
      expect(request.request.headers.has('Authorization')).toBe(false);
      request.flush({});
    }
    expect(keycloak.updateToken).not.toHaveBeenCalled();
  });

  it('no envía peticiones autenticadas si no hay token', async () => {
    keycloak.token = undefined;
    const result = firstValueFrom(TestBed.inject(CurrentUserService).getMe());
    await expect(result).rejects.toThrow('No hay una sesion autenticada.');
    TestBed.inject(HttpTestingController).expectNone(`${environment.apiUrl}/me`);
  });
});
