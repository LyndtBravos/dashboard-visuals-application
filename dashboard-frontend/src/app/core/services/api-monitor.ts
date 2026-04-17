import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ApiMonitor {
  id?: number;
  name: string;
  description?: string;
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH' | 'HEAD' | 'OPTIONS';
  requestHeadersJson?: string;
  requestBody?: string;
  requestContentType?: string;
  expectedStatusCode: number;
  expectedResponseTimeMs?: number;
  expectedResponseSizeBytes?: number;
  expectedResponseContains?: string;
  expectedJsonPath?: string;
  expectedValue?: string;
  timeoutSeconds: number;
  retryCount: number;
  retryIntervalSeconds: number;
  checkIntervalMinutes: number;
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
  lastResponseTimeMs?: number;
  lastResponseSizeBytes?: number;
  lastResponseBody?: string;
  currentFailureCount?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ApiMonitorService {
  private apiUrl = 'http://localhost:8080/api/monitors/apis';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ApiMonitor[]> {
    return this.http.get<ApiMonitor[]>(this.apiUrl);
  }

  getById(id: number): Observable<ApiMonitor> {
    return this.http.get<ApiMonitor>(`${this.apiUrl}/${id}`);
  }

  create(data: ApiMonitor): Observable<ApiMonitor> {
    return this.http.post<ApiMonitor>(this.apiUrl, data);
  }

  update(id: number, data: ApiMonitor): Observable<ApiMonitor> {
    return this.http.put<ApiMonitor>(`${this.apiUrl}/${id}`, data);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  resetFailureCount(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/reset`, {});
  }

  getFailing(): Observable<ApiMonitor[]> {
    return this.http.get<ApiMonitor[]>(`${this.apiUrl}/failing`);
  }

  getAlerting(): Observable<ApiMonitor[]> {
    return this.http.get<ApiMonitor[]>(`${this.apiUrl}/alerting`);
  }

  recheck(id: number): Observable<ApiMonitor> {
    return this.http.post<ApiMonitor>(`${this.apiUrl}/${id}/recheck`, {});
  }
}