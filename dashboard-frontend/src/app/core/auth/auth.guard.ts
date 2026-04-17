import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from './auth';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard {
  private isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }
  
  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): boolean | UrlTree {
    const isLoginRoute = route.data['guard'] === 'login';
    const isAuthenticated = this.authService.isAuthenticated();
    
    if (isLoginRoute) {
      if (isAuthenticated) {
        const redirectUrl = localStorage.getItem('redirectUrl');
        if (redirectUrl && redirectUrl !== '/login' && redirectUrl !== '/register') {
          localStorage.removeItem('redirectUrl');
          console.log('Redirecting to stored URL:', redirectUrl);
          return this.router.parseUrl(redirectUrl);
        }
        
        return this.router.parseUrl('/dashboard');
      }
      return true;
    }
    
    if (!isAuthenticated) {
      if (this.isBrowser && state.url !== '/login' && state.url !== '/register') 
        localStorage.setItem('redirectUrl', state.url);

      return this.router.parseUrl('/login');
    }
    
    if (this.isBrowser && state.url !== '/login' && state.url !== '/register')
      localStorage.removeItem('redirectUrl');
        
    return true;
  }
}