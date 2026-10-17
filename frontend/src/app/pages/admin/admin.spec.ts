import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import {
  ActivatedRouteSnapshot,
  provideRouter,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import { adminGuard } from '../../auth/admin.guard';
import { keycloak } from '../../auth/keycloak';
import { Admin } from './admin';
import { Business } from '../../core/models/business.model';
import { environment } from '../../../environments/environment';

const createdBusiness: Business = {
  id: 'business-id',
  name: 'Café Olivo',
  status: 'ACTIVE',
  createdAt: '2026-10-17T10:00:00Z',
  updatedAt: '2026-10-17T10:00:00Z',
};

describe('Panel de administración', () => {
  beforeEach(async () => {
    keycloak.authenticated = true;
    keycloak.tokenParsed = { preferred_username: 'admin' };
    vi.spyOn(keycloak, 'hasRealmRole').mockImplementation((role) => role === 'admin');
    Object.defineProperty(HTMLDialogElement.prototype, 'showModal', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = true;
      },
    });
    Object.defineProperty(HTMLDialogElement.prototype, 'close', {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = false;
      },
    });
    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    vi.restoreAllMocks();
    keycloak.authenticated = false;
    keycloak.tokenParsed = undefined;
  });

  it('permite administradores y rechaza usuarios sin rol o sin sesión', () => {
    const guard = () =>
      TestBed.runInInjectionContext(() =>
        adminGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
      );
    expect(guard()).toBe(true);
    vi.mocked(keycloak.hasRealmRole).mockReturnValue(false);
    expect(TestBed.inject(Router).serializeUrl(guard() as ReturnType<Router['parseUrl']>)).toBe(
      '/',
    );
    vi.mocked(keycloak.hasRealmRole).mockReturnValue(true);
    keycloak.authenticated = false;
    expect(TestBed.inject(Router).serializeUrl(guard() as ReturnType<Router['parseUrl']>)).toBe(
      '/',
    );
  });

  it('rechaza nombres vacíos sin enviar solicitudes', () => {
    const fixture = TestBed.createComponent(Admin);
    fixture.detectChanges();
    const form = fixture.componentInstance.createForm();
    form.open();
    form.form.setValue({ name: '   ', ownerKeycloakUserId: 'owner-id' });
    form.submit();
    expect(form.form.controls.name.invalid).toBe(true);
    TestBed.inject(HttpTestingController).expectNone(`${environment.apiUrl}/admin/businesses`);
  });

  it('crea una sola vez, muestra el resultado y permite buscarlo', () => {
    const fixture = TestBed.createComponent(Admin);
    fixture.detectChanges();
    const page = fixture.componentInstance;
    const form = page.createForm();
    form.open();
    form.form.setValue({ name: '  Café Olivo  ', ownerKeycloakUserId: ' owner-id ' });
    form.submit();
    form.submit();
    expect(form.saving()).toBe(true);
    const event = new Event('cancel', { cancelable: true });
    form.cancel(event);
    expect(event.defaultPrevented).toBe(true);
    const request = TestBed.inject(HttpTestingController).expectOne(
      `${environment.apiUrl}/admin/businesses`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Café Olivo', ownerKeycloakUserId: 'owner-id' });
    request.flush(createdBusiness);
    fixture.detectChanges();
    expect(form.saving()).toBe(false);
    expect(fixture.nativeElement.querySelector('dialog').open).toBe(false);
    expect(fixture.nativeElement.querySelector('tbody')?.textContent).toContain('Café Olivo');
    expect(page.success()).toContain('se creó correctamente');
    page.search.set('otro');
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No encontramos coincidencias');
    page.clearFilters();
    expect(page.filteredBusinesses()).toEqual([createdBusiness]);
  });

  it('conserva los datos ante un conflicto y permite corregirlos', () => {
    const fixture = TestBed.createComponent(Admin);
    fixture.detectChanges();
    const form = fixture.componentInstance.createForm();
    form.open();
    form.form.setValue({ name: 'Café Olivo', ownerKeycloakUserId: 'owner-id' });
    form.submit();
    TestBed.inject(HttpTestingController)
      .expectOne(`${environment.apiUrl}/admin/businesses`)
      .flush({ code: 'Membership.UserAlreadyAssigned' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(form.error()).toContain('ya pertenece a un negocio');
    expect(form.form.getRawValue().ownerKeycloakUserId).toBe('owner-id');
    expect(form.form.enabled).toBe(true);
    expect(fixture.nativeElement.querySelector('dialog').open).toBe(true);
    expect(fixture.componentInstance.businesses()).toEqual([]);
  });
});
