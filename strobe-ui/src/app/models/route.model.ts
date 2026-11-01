/**
 * Data contracts representing dynamic gateway route specifications.
 */

export interface StrobeRoute {
  id: string;
  uri: string;
  path: string;
  predicates: string[];
  filters: string[];
  enabled: boolean;
}
