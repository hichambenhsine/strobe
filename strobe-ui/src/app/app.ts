import {ChangeDetectionStrategy, Component, computed, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';
import {StrobeHealthService} from './services/health.service';
import {DiskSpaceDetails, StrobeHealthResponse} from './models/health.model';

interface ComponentViewModel {
  name: string;
  status: string;
}

/**
 * Root developer console component for Strobe.
 * Implements modern Angular Signals to manage application probe state.
 */
@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App implements OnInit{
  private readonly healthService = inject(StrobeHealthService);

  public readonly healthData = signal<StrobeHealthResponse | null>(null)
  public readonly loading = signal<boolean>(true);
  public readonly error = signal<string | null>(null);

  /**
   * Computed projection converting the components map into an iterable list for templates.
   */
  public readonly componentList = computed<ComponentViewModel[]>(() => {
    const data = this.healthData();
    if (!data?.components) return [];

    return Object.entries(data.components).map(([name, item]) => ({
      name,
      status: item.status
    }));
  });

  /**
   * Computed disk metrics converting raw bytes into human-readable gigabytes and usage ratios.
   */
  public readonly diskMetrics = computed(() => {
    const data = this.healthData();
    const disk = data?.components?.['diskSpace']?.details as DiskSpaceDetails | undefined;
    if (!disk || !disk.total) return null;

    const totalGb = (disk.total / 1073741824).toFixed(1);
    const freeGb = (disk.free / 1073741824).toFixed(1);
    const usedBytes = disk.total - disk.free;
    const usedGb = (usedBytes / 1073741824).toFixed(1);
    const usagePercent = Math.round((usedBytes / disk.total) * 100);

    return { totalGb, freeGb, usedGb, usagePercent };
  });

  ngOnInit(): void {
    this.refreshHealthTelemetry();
  }

  public refreshHealthTelemetry(): void {
    this.loading.set(true);
    this.error.set(null);

    this.healthService.getHealth().subscribe({
      next: (response) => {
        this.healthData.set(response);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to connect to Strobe health endpoint');
        this.loading.set(false);
      }
    });
  }
}
