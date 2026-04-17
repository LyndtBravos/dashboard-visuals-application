import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8080/api';
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }
  
  private setToken(token: string): void {
    if (this.isBrowser) localStorage.setItem('accessToken', token);
  }
  
  private setRefreshToken(token: string): void {
    if (this.isBrowser) localStorage.setItem('refreshToken', token);
  }
  
  private setUser(user: any): void {
    if (this.isBrowser) localStorage.setItem('user', JSON.stringify(user));
  }
  
  getToken(): string | null {
    if (this.isBrowser) return localStorage.getItem('accessToken');
    return null;
  }
  
  getRefreshToken(): string | null {
    if (this.isBrowser) return localStorage.getItem('refreshToken');
    return null;
  }
  
  getUser(): any {
    if (this.isBrowser) {
      const user = localStorage.getItem('user');
      return user ? JSON.parse(user) : null;
    }
    return null;
  }
  
  private removeTokens(): void {
    if (this.isBrowser) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('user');
    }
  }

  login(credentials: { userId: string; password: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/login`, credentials);
  }
  
  register(user: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/auth/register`, user);
  }
  
  logout(): void {
    this.removeTokens();
  }
  
  saveAuthData(response: any): void {
    if (response.accessToken) {
      this.setToken(response.accessToken);
      if (response.refreshToken) this.setRefreshToken(response.refreshToken);
      this.setUser({
        userId: response.userId,
        name: response.name,
        level: response.level,
        email: response.email
      });
    }
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    if (!token) return false;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expired = payload.exp * 1000 < Date.now();
      if (expired) {
        this.logout();
        return false;
      }
      return true;
    } catch (e) {
      return !!token;
    }
  }

  getCurrentUser(): any {
    return this.getUser();
  }
}