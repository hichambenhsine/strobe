import {inject, Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {StrobeRoute} from '../models/route.model';

/**
 * Service orchestrating dynamic route operations with the Strobe starter API.
 */
@Injectable({
  providedIn: 'root'
})
export class StrobeRouteService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/strobe/api/routes';

  /**
   * Retrieves all registered gateway routes.
   *
   * @return Observable emitting array of StrobeRoute objects.
   */
  public getRoutes(): Observable<StrobeRoute[]> {
    return this.http.get<StrobeRoute[]>(this.apiUrl);
  }

  /**
   * Dispatches a new route configuration for dynamic registration.
   *
   * @param route Route payload to persist.
   * @return Observable emitting the created StrobeRoute.
   */
  public createRoute(route: StrobeRoute): Observable<StrobeRoute> {
    return this.http.post<StrobeRoute>(this.apiUrl, route);
  }

  /**
   * Deletes a dynamic route by its unique identifier.
   *
   * @param routeId Unique route identifier.
   * @return Observable completing upon deletion.
   */
  public deleteRoute(routeId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${routeId}`);
  }

  /**
   * Toggles the active status of a target route.
   *
   * @param routeId Unique route identifier.
   * @return Observable emitting the updated StrobeRoute.
   */
  public toggleRoute(routeId: string): Observable<StrobeRoute> {
    return this.http.put<StrobeRoute>(`${this.apiUrl}/${routeId}/toggle`, {});
  }
}
