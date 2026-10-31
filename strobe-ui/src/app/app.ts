import {ChangeDetectionStrategy, Component, inject, OnInit, signal} from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {HttpClient} from '@angular/common/http';

interface PingResponse {
  status: string;
  message: string;
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
  private readonly http = inject(HttpClient);

  public readonly pingData = signal<PingResponse | null>(null)
  public readonly loading = signal<boolean>(true);
  public readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.refreshProbe();
  }

  public refreshProbe(): void {
    this.loading.set(true);
    this.error.set(null);

    this.http.get<PingResponse>('/strobe/api/ping').subscribe({
      next: (data) => {
        this.pingData.set(data);
        this.loading.set(false);
      },
      error: ()=> {
        this.error.set('Could not communicate with Strobe starter backend');
        this.loading.set(false);
      }
    })
  }
}
