import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {StrobeRouteService} from '../../services/route.service';
import {StrobeRoute} from '../../models/route.model';

/**
 * Component managing live gateway route configurations.
 * Supports on-the-fly route registration, state toggling, and deletion.
 */
@Component({
  imports: [ReactiveFormsModule],
  selector: 'app-route-manager',
  styleUrl: './route-manager.scss',
  templateUrl: './route-manager.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RouteManager {
  private readonly routeService = inject(StrobeRouteService);
  private readonly fb = inject(FormBuilder);

  public readonly routes = signal<StrobeRoute[]>([]);
  public readonly loading = signal<boolean>(true);
  public readonly showModal = signal<boolean>(false);

  public readonly routeForm: FormGroup = this.fb.group({
    id: ['', [Validators.required, Validators.pattern(/^[a-zA-Z0-9_-]+$/)]],
    uri: ['', [Validators.required]],
    path: ['', [Validators.required]],
    predicates: [''],
    filters: [''],
    enabled: [true]
  });

  ngOnInit(): void {
    this.fetchRoutes();
  }

  public fetchRoutes(): void {
    this.loading.set(true);
    this.routeService.getRoutes().subscribe({
      next: (data) => {
        this.routes.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  public toggleRoute(routeId: string): void {
    this.routeService.toggleRoute(routeId).subscribe({
      next: (updated) => {
        this.routes.update((current) =>
          current.map((r) => (r.id === updated.id ? updated : r))
        );
      }
    });
  }

  public deleteRoute(routeId: string): void {
    this.routeService.deleteRoute(routeId).subscribe({
      next: () => {
        this.routes.update((current) => current.filter((r) => r.id !== routeId));
      }
    });
  }

  public openModal(): void {
    this.routeForm.reset({ enabled: true });
    this.showModal.set(true);
  }

  public closeModal(): void {
    this.showModal.set(false);
  }

  public submitRoute(): void {
    if (this.routeForm.invalid) return;

    const val = this.routeForm.value;
    const predicates = val.predicates ? val.predicates.split(',').map((s: string) => s.trim()) : [];
    const filters = val.filters ? val.filters.split(',').map((s: string) => s.trim()) : [];

    const newRoute: StrobeRoute = {
      id: val.id,
      uri: val.uri,
      path: val.path,
      predicates,
      filters,
      enabled: val.enabled
    };

    this.routeService.createRoute(newRoute).subscribe({
      next: (created) => {
        this.routes.update((current) => [...current, created]);
        this.closeModal();
      }
    });
  }
}
