import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AlertDetail {
  id: number;
  monitorType: 'site' | 'api' | 'server' | 'query';
  monitorId: number;
  name: string;
  severity: 'critical' | 'high' | 'medium' | 'low';
  failureReason: string;
  startedAt: string;
  lastOccurrence: string;
  occurrenceCount: number;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
  
  // Monitor-specific
  url?: string;
  method?: string;
  host?: string;
  port?: number;
  protocol?: string;
  queryText?: string;
  graphType?: string;
  serviceType?: string;
  expectedStatusCode?: number;
  currentFailureCount?: number;
  retryCount?: number;
  lastCheckMessage?: string;
  lastCheckTime?: string;
}

export interface Alert {
  id: number;
  monitorType: 'site' | 'api' | 'server';
  monitorId: number;
  name: string;
  severity: 'critical' | 'high' | 'medium' | 'low';
  failureReason: string;
  startedAt: string;
  lastOccurrence: string;
  occurrenceCount: number;
  acknowledged: boolean;
  acknowledgedBy?: string;
  acknowledgedAt?: string;
  resolvedAt?: string;
}

export interface AlertCounts {
  total: number;
  critical: number;
  high: number;
  medium: number;
  low: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = 'http://localhost:8080/api/alerts';

  constructor(private http: HttpClient) {}

  getActiveAlerts(): Observable<Alert[]> {
    return this.http.get<Alert[]>(`${this.apiUrl}/active`);
  }

  getAlertDetail(id: number): Observable<AlertDetail> {
    return this.http.get<AlertDetail>(`${this.apiUrl}/${id}/detail`);
  }

  getAllAlerts(acknowledged?: boolean, status?: string): Observable<Alert[]> {
    let url = this.apiUrl;
    const params: string[] = [];
    
    if (acknowledged !== undefined) 
      params.push(`acknowledged=${acknowledged}`);
    
    if (status) 
      params.push(`status=${status}`);
    
    
    if (params.length) 
      url += `?${params.join('&')}`;
        
    return this.http.get<Alert[]>(url);
  }

  getAlertById(id: number): Observable<Alert> {
    return this.http.get<Alert>(`${this.apiUrl}/${id}`);
  }

  acknowledgeAlert(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/acknowledge`, {});
  }

  resolveAlert(id: number): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${id}/resolve`, {});
  }

  getAlertCounts(): Observable<AlertCounts> {
    return this.http.get<AlertCounts>(`${this.apiUrl}/counts`);
  }
}