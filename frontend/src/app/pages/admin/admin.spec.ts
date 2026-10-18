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

const endpoint = `${environment.apiUrl}/admin/businesses`;

function flushList(items: Business[] = [], total = items.length, page = 0) {
  const request = TestBed.inject(HttpTestingController).expectOne(
    (request) => request.url === endpoint && request.method === 'GET',
  );
  request.flush({ items, page, size: 10, total, totalPages: Math.ceil(total / 10) });
  return request;
}

function renderPage(items: Business[] = [], total = items.length) {
  const fixture = TestBed.createComponent(Admin);
  fixture.detectChanges();
  flushList(items, total);
  fixture.detectChanges();
  return fixture;
}

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

  it('carga negocios guardados al entrar y al volver al panel', () => {
    const first = renderPage([createdBusiness]);
    expect(first.nativeElement.querySelector('tbody')?.textContent).toContain('Café Olivo');
    first.destroy();
    const second = renderPage([createdBusiness]);
    expect(second.componentInstance.total()).toBe(1);
    expect(second.nativeElement.querySelector('tbody')?.textContent).toContain('Café Olivo');
  });

  it('rechaza nombres vacíos sin enviar solicitudes de creación', () => {
    const fixture = renderPage();
    const form = fixture.componentInstance.createForm();
    form.open();
    form.form.setValue({ name: '   ', ownerKeycloakUserId: 'owner-id' });
    form.submit();
    expect(form.form.controls.name.invalid).toBe(true);
    TestBed.inject(HttpTestingController).expectNone((request) => request.method === 'POST');
  });

  it('crea una sola vez y vuelve a consultar el listado guardado', () => {
    const fixture = renderPage();
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
    const request = TestBed.inject(HttpTestingController).expectOne(endpoint);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Café Olivo', ownerKeycloakUserId: 'owner-id' });
    request.flush(createdBusiness);
    expect(page.loading()).toBe(true);
    flushList([createdBusiness]);
    fixture.detectChanges();
    expect(form.saving()).toBe(false);
    expect(fixture.nativeElement.querySelector('dialog').open).toBe(false);
    expect(fixture.nativeElement.querySelector('tbody')?.textContent).toContain('Café Olivo');
    expect(page.success()).toContain('se creó correctamente');
  });

  it('consulta filtros y páginas en el servidor y cancela respuestas anteriores', () => {
    const fixture = renderPage([createdBusiness], 21);
    const page = fixture.componentInstance;
    const http = TestBed.inject(HttpTestingController);
    page.search.set('sin aplicar');
    page.goToPage(1);
    const next = http.expectOne((request) => request.url === endpoint);
    expect(next.request.params.get('page')).toBe('1');
    expect(next.request.params.get('size')).toBe('10');
    expect(next.request.params.has('search')).toBe(false);
    next.flush({ items: [createdBusiness], page: 1, size: 10, total: 21, totalPages: 3 });
    page.search.set('  Olivo  ');
    page.status.set('SUSPENDED');
    page.applyFilters();
    const filtered = http.expectOne((request) => request.url === endpoint);
    expect(filtered.request.params.get('page')).toBe('0');
    expect(filtered.request.params.get('search')).toBe('Olivo');
    expect(filtered.request.params.get('status')).toBe('SUSPENDED');
    page.clearFilters();
    expect(filtered.cancelled).toBe(true);
    const cleared = flushList([createdBusiness], 21);
    expect(cleared.request.params.has('search')).toBe(false);
    expect(cleared.request.params.has('status')).toBe(false);
    expect(page.page()).toBe(0);
  });

  it('vuelve a la última página si el total disminuye mientras se consulta', () => {
    const fixture = renderPage([createdBusiness], 21);
    const page = fixture.componentInstance;
    page.goToPage(2);
    flushList([], 11, 2);
    const corrected = flushList([createdBusiness], 11, 1);
    expect(corrected.request.params.get('page')).toBe('1');
    expect(page.page()).toBe(1);
    expect(page.totalPages()).toBe(2);
    expect(page.businesses()).toEqual([createdBusiness]);
  });

  it('muestra errores de consulta y permite reintentar', () => {
    const fixture = TestBed.createComponent(Admin);
    fixture.detectChanges();
    expect(fixture.componentInstance.loading()).toBe(true);
    TestBed.inject(HttpTestingController)
      .expectOne((request) => request.url === endpoint)
      .flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar los negocios');
    fixture.componentInstance.load();
    flushList([createdBusiness]);
    fixture.detectChanges();
    expect(fixture.componentInstance.error()).toBe('');
    expect(fixture.nativeElement.querySelector('tbody')?.textContent).toContain('Café Olivo');
  });

  it('conserva los datos ante un conflicto y permite corregirlos', () => {
    const fixture = renderPage();
    const form = fixture.componentInstance.createForm();
    form.open();
    form.form.setValue({ name: 'Café Olivo', ownerKeycloakUserId: 'owner-id' });
    form.submit();
    TestBed.inject(HttpTestingController)
      .expectOne(endpoint)
      .flush({ code: 'Membership.UserAlreadyAssigned' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(form.error()).toContain('ya pertenece a un negocio');
    expect(form.form.getRawValue().ownerKeycloakUserId).toBe('owner-id');
    expect(form.form.enabled).toBe(true);
    expect(fixture.nativeElement.querySelector('dialog').open).toBe(true);
    expect(fixture.componentInstance.businesses()).toEqual([]);
  });
});
