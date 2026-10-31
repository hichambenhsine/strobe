import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {StrobeHealthResponse} from '../models/health.model';

/**
 * Service responsible for fetching telemetry and health diagnostics from Strobe backend.
 */
@Injectable({
  providedIn: 'root'
})
export class StrobeHealthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/strobe/api/health';

  /**
   * Retrieves aggregated system health metrics from the backend probe.
   *
   * @return Observable emitting the parsed StrobeHealthResponse.
   */
  public getHealth(): Observable<StrobeHealthResponse> {
    return this.http.get<StrobeHealthResponse>(this.apiUrl);
  }
}
