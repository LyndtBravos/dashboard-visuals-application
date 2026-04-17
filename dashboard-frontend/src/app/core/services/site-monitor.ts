import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SiteMonitor {
  id?: number;
  name: string;
  description?: string;
  url: string;
  expectedPhrase?: string;
  expectedPhraseMissing?: boolean;
  retryCount: number;
  retryIntervalSeconds: number;
  checkIntervalMinutes: number;
  timeoutSeconds: number;
  followRedirects?: boolean;
  expectedStatusCode: number;
  serviceType: 'Alerts' | 'Broadcast' | 'Deliveries' | 'Online' | 'Print';
  severity: 'critical' | 'high' | 'medium' | 'low';
  businessHoursOnly?: boolean;
  businessHoursStart?: string;
  businessHoursEnd?: string;
  businessDays?: string;
  isActive?: boolean;
  alert?: boolean;
  alertEmail?: string;
  lastCheckTime?: string;
  lastCheckStatus?: string;
  lastCheckMessage?: string;
  currentFailureCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class SiteMonitorService {
  private apiUrl = 'http://localhost:8080/api/monitors/sites';

  constructor(private http: HttpClient) {}

  getAll(): Observable<SiteMonitor[]> {
    return this.http.get<SiteMonitor[]>(this.apiUrl);
  }

  getById(id: number): Observable<SiteMonitor> {
    return this.http.get<SiteMonitor>(`${this.apiUrl}/${id}`);
  }

  create(data: SiteMonitor): Observable<SiteMonitor> {
    return this.http.post<SiteMonitor>(this.apiUrl, data);
  }

  update(id: number, data: SiteMonitor): Observable<SiteMonitor> {
    return this.http.put<SiteMonitor>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  resetFailureCount(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/reset`, {});
  }

  getFailing(): Observable<SiteMonitor[]> {
    return this.http.get<SiteMonitor[]>(`${this.apiUrl}/failing`);
  }

  getAlerting(): Observable<SiteMonitor[]> {
    return this.http.get<SiteMonitor[]>(`${this.apiUrl}/alerting`);
  }

  recheck(id: number): Observable<SiteMonitor> {
    return this.http.post<SiteMonitor>(`${this.apiUrl}/${id}/recheck`, {});
  }
}