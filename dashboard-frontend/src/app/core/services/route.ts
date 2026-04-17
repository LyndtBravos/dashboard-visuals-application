import { Injectable } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class RouteService {
  private previousUrl: string | null = null;
  private currentUrl: string | null = null;
  
  constructor(private router: Router) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.previousUrl = this.currentUrl;
      this.currentUrl = event.urlAfterRedirects;
    });
  }
  
  getPreviousUrl(): string | null {
    return this.previousUrl;
  }
  
  getCurrentUrl(): string | null {
    return this.currentUrl;
  }
  
  storeCurrentUrl(): void {
    if (this.currentUrl && this.currentUrl !== '/login' && this.currentUrl !== '/register') {
      localStorage.setItem('lastVisitedUrl', this.currentUrl);
    }
  }
  
  getLastVisitedUrl(): string | null {
    return localStorage.getItem('lastVisitedUrl');
  }
  
  clearLastVisitedUrl(): void {
    localStorage.removeItem('lastVisitedUrl');
  }
}