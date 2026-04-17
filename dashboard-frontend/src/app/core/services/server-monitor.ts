import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ServerMonitor {
  id?: number;
  name: string;
  description?: string;
  host: string;
  port?: number;
  protocol: 'icmp' | 'tcp' | 'http' | 'https';
  timeoutSeconds: number;
  retryCount: number;
  retryIntervalSeconds: number;
  checkIntervalMinutes: number;
  serviceType: 'Broadcast' | 'Print' | 'Online' | 'Deliveries' | 'Alerts' | 'Other';
  severity: 'critical' | 'high' | 'medium' | 'low';
  businessHoursOnly?: boolean;
  businessHoursStart?: string;
  businessHoursEnd?: string;
  businessDays?: string;
  isActive?: boolean;
  alert?: boolean;
  alertEmail?: string;
  lastCheckTime?: string;
  lastCheckStatus?: 'success' | 'failed' | 'error' | 'pending';
  lastCheckMessage?: string;
  responseTimeMs?: number;
  currentFailureCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ServerMonitorService {
  private apiUrl = 'http://localhost:8080/api/monitors/servers';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ServerMonitor[]> {
    return this.http.get<ServerMonitor[]>(this.apiUrl);
  }

  getById(id: number): Observable<ServerMonitor> {
    return this.http.get<ServerMonitor>(`${this.apiUrl}/${id}`);
  }

  create(data: ServerMonitor): Observable<ServerMonitor> {
    return this.http.post<ServerMonitor>(this.apiUrl, data);
  }

  update(id: number, data: ServerMonitor): Observable<ServerMonitor> {
    return this.http.put<ServerMonitor>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  resetFailureCount(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/reset`, {});
  }

  recheck(id: number): Observable<ServerMonitor> {
    return this.http.post<ServerMonitor>(`${this.apiUrl}/${id}/recheck`, {});
  }

  getFailing(): Observable<ServerMonitor[]> {
    return this.http.get<ServerMonitor[]>(`${this.apiUrl}/failing`);
  }

  getAlerting(): Observable<ServerMonitor[]> {
    return this.http.get<ServerMonitor[]>(`${this.apiUrl}/alerting`);
  }
}