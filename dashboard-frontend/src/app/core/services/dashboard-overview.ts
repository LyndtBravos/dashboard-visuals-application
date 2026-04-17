import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ServiceSummary {
  serviceType: 'Broadcast' | 'Print' | 'Online' | 'Deliveries' | 'Alerts';
  totalVisuals: number;
  statusCounts: {
    green: number;
    yellow: number;
    red: number;
    inactive: number;
  };
  timestamp: string;
}

export interface DashboardConfig {
  id?: number;
  name: string;
  description?: string;
  serviceType: string;
  graphType: string;
  queryText: string;
  thresholdWarning?: number;
  thresholdDanger?: number;
  alertEmail?: string;
  alert?: boolean;
  width?: string;
  height?: string;
  customWidth?: number;
  customHeight?: number;
  isActive?: boolean;
  currentValue?: any;
  currentStatus?: 'green' | 'yellow' | 'red';
  lastChecked?: string;
  flowOrder?: number;
  
  xAxisColumn?: string;
  yAxisColumns?: string;
  seriesColumns?: string;
  aggregationType?: string;
  timeInterval?: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = 'http://localhost:8080/api/dashboards';

  constructor(private http: HttpClient) {}

  getAllServicesSummary(): Observable<ServiceSummary[]> {
    return this.http.get<ServiceSummary[]>(`${this.apiUrl}/summary`);
  }

  getDashboardsByService(serviceType: string): Observable<DashboardConfig[]> {
    return this.http.get<DashboardConfig[]>(`${this.apiUrl}/service/${serviceType}`);
  }

  getDashboard(id: number): Observable<DashboardConfig> {
    return this.http.get<DashboardConfig>(`${this.apiUrl}/${id}`);
  }

  createDashboard(data: Partial<DashboardConfig>): Observable<DashboardConfig> {
    return this.http.post<DashboardConfig>(`${this.apiUrl}`, data);
  }

  updateDashboard(id: number, data: Partial<DashboardConfig>): Observable<DashboardConfig> {
    return this.http.put<DashboardConfig>(`${this.apiUrl}/${id}`, data);
  }

  deleteDashboard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getOrderedDashboardsByService(serviceType: string): Observable<DashboardConfig[]> {
    return this.http.get<DashboardConfig[]>(`${this.apiUrl}/service/${serviceType}/ordered`);
  }

  sendEmailReport(payload: any): Observable<any> {
    return this.http.post(`http://localhost:8080/api/email/send-report`, payload);
  }
}