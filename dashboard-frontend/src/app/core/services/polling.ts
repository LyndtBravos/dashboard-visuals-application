import { Injectable, OnDestroy } from '@angular/core';
import { BehaviorSubject, interval, Subscription } from 'rxjs';

export interface PollingConfig {
  enabled: boolean;
  intervalMs: number;
  lastRun: Date | null;
}

@Injectable({
  providedIn: 'root'
})
export class PollingService implements OnDestroy {
  private pollingSubscription: Subscription | null = null;
  private refreshCallback: (() => void) | null = null;
  private config: PollingConfig = {
    enabled: false,
    intervalMs: 30000,
    lastRun: null
  };
  
  private pollingStatusSubject = new BehaviorSubject<PollingConfig>(this.config);
  pollingStatus$ = this.pollingStatusSubject.asObservable();
  
  readonly availableIntervals = [
    { value: 0, label: 'Off', seconds: 0 },
    { value: 5000, label: '5 seconds', seconds: 5 },
    { value: 10000, label: '10 seconds', seconds: 10 },
    { value: 15000, label: '15 seconds', seconds: 15 },
    { value: 30000, label: '30 seconds', seconds: 30 },
    { value: 60000, label: '1 minute', seconds: 60 },
    { value: 120000, label: '2 minutes', seconds: 120 },
    { value: 300000, label: '5 minutes', seconds: 300 }
  ];
  
  ngOnDestroy(): void {
    this.stopPolling();
  }
  
  startPolling(callback: () => void, intervalMs: number = 30000): void {
    this.stopPolling();
    
    this.refreshCallback = callback;
    
    this.config.enabled = true;
    this.config.intervalMs = intervalMs;
    this.config.lastRun = null;
    
    this.pollingSubscription = interval(intervalMs).subscribe(() => {
      if (this.refreshCallback) {
        this.config.lastRun = new Date();
        this.pollingStatusSubject.next({ ...this.config });
        this.refreshCallback();
      }
    });
    
    this.pollingStatusSubject.next({ ...this.config });
    console.log(`Polling started with interval ${intervalMs}ms (${intervalMs / 1000} seconds)`);
  }
  
  stopPolling(): void {
    if (this.pollingSubscription) {
      this.pollingSubscription.unsubscribe();
      this.pollingSubscription = null;
    }
    
    this.refreshCallback = null;
    this.config.enabled = false;
    this.config.lastRun = null;
    
    this.pollingStatusSubject.next({ ...this.config });
  }
  
  changeInterval(intervalMs: number): void {
    if (this.config.enabled && this.refreshCallback) 
      this.startPolling(this.refreshCallback, intervalMs);
    else {
      this.config.intervalMs = intervalMs;
      this.pollingStatusSubject.next({ ...this.config });
    }
  }
  
  isPollingActive(): boolean {
    return this.config.enabled;
  }
  
  getConfig(): PollingConfig {
    return { ...this.config };
  }
  
  getCurrentInterval(): number {
    return this.config.intervalMs;
  }
  
  getIntervalLabel(intervalMs: number): string {
    const found = this.availableIntervals.find(i => i.value === intervalMs);
    return found ? found.label : `${intervalMs / 1000} seconds`;
  }
  
  manualRefresh(): void {
    if (this.refreshCallback) {
      this.config.lastRun = new Date();
      this.pollingStatusSubject.next({ ...this.config });
      this.refreshCallback();
    } else 
      console.warn('No callback registered for manual refresh');
  }
  
  togglePolling(callback: () => void, intervalMs: number = 30000): void {
    if (this.config.enabled)
      this.stopPolling();
    else
      this.startPolling(callback, intervalMs);
  }
}