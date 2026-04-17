import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface QueryResult {
  value: any;
  status: 'green' | 'yellow' | 'red' | 'error';
  warningThreshold?: number;
  dangerThreshold?: number;
  query: string;
  timestamp: string;
  errorMessage?: string;
}

export interface QueryDataSet {
  success: boolean;
  data: Array<Record<string, any>>;
  columns: string[];
  rowCount: number;
  query: string;
  timestamp: string;
  errorMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class QueryService {
  private apiUrl = 'http://localhost:8080/api/query';

  constructor(private http: HttpClient) {}

  executeQuery(query: string, warningThreshold?: number, dangerThreshold?: number): Observable<QueryResult> {
    if (!query || query.trim() === '')
      throw new Error('Query cannot be empty');
    
    const body = {
      query: query.trim(),
      warningThreshold: warningThreshold !== undefined && warningThreshold !== null ? warningThreshold : null,
      dangerThreshold: dangerThreshold !== undefined && dangerThreshold !== null ? dangerThreshold : null
    };
    
    return this.http.post<QueryResult>(`${this.apiUrl}/execute`, body, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    });
  }

  executeQueryDataset(query: string, warningThreshold?: number, dangerThreshold?: number): Observable<QueryDataSet> {
    if (!query || query.trim() === '')
      throw new Error('Query cannot be empty');
        
    const body = {
      query: query.trim(),
      warningThreshold: warningThreshold !== undefined && warningThreshold !== null ? warningThreshold : null,
      dangerThreshold: dangerThreshold !== undefined && dangerThreshold !== null ? dangerThreshold : null
    };
    
    return this.http.post<QueryDataSet>(`${this.apiUrl}/execute-dataset`, body, {
      headers: new HttpHeaders({
        'Content-Type': 'application/json'
      })
    });
  }

  recheckQuery(query: string): Observable<QueryDataSet> {
    return this.http.post<QueryDataSet>(`http://localhost:8080/api/query/recheck`, {query: query});
  }
}