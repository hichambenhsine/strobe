/**
 * Data contracts representing system health telemetry.
 */

export interface DiskSpaceDetails {
  total: number;
  free: number;
  threshold: number;
  exists?: boolean;
}

export interface SubsystemHealth {
  status: string;
  details?: DiskSpaceDetails | Record<string, unknown>
}

export interface StrobeHealthResponse {
  status: string;
  components: Record<string, SubsystemHealth>;
}
