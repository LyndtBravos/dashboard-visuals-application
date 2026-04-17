import { Component, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { NotificationToastComponent } from './shared/components/notification-toast/notification-toast';
import { GlobalLoaderComponent } from './shared/components/global-loader/global-loader';
import { filter } from 'rxjs/operators';
import { ConfirmModalComponent } from './shared/components/confirm-modal/confirm-modal';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NotificationToastComponent, GlobalLoaderComponent, ConfirmModalComponent],
  template: `<router-outlet></router-outlet><app-notification-toast></app-notification-toast><app-global-loader></app-global-loader><app-confirm-modal></app-confirm-modal>`,
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
  title = 'dashboard-visuals';
  
  constructor(private router: Router){}
  
  ngOnInit(): void {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      if (event.urlAfterRedirects !== '/login' && event.urlAfterRedirects !== '/register')
        localStorage.setItem('lastVisitedUrl', event.urlAfterRedirects);
    });
  }
}