import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/login/login';
import { RegisterComponent } from './features/auth/register/register';
import { MainLayoutComponent } from './layouts/main-layout/main-layout';
import { ServiceDetailComponent } from './features/dashboard/service-detail/service-detail.component';
import { SiteMonitorsComponent } from './features/monitors/site-monitors/site-monitors';
import { ApiMonitorsComponent } from './features/monitors/api-monitors/api-monitors';
import { AlertsComponent } from './features/monitors/alerts/alerts';
import { ServerMonitorsComponent } from './features/monitors/server-monitors/server-monitors';
import { AuthGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { 
    path: 'login', 
    component: LoginComponent,
    canActivate: [AuthGuard],
    data: { guard: 'login' }
  },
  { 
    path: 'register', 
    component: RegisterComponent,
    canActivate: [AuthGuard],
    data: { guard: 'login' }
  },
  
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: '/monitors/apis', pathMatch: 'full' },
      { path: 'dashboard', component: ServiceDetailComponent },
      { path: 'dashboard/:type', component: ServiceDetailComponent },
      { path: 'monitors/sites', component: SiteMonitorsComponent },
      { path: 'monitors/apis', component: ApiMonitorsComponent },
      { path: 'monitors/servers', component: ServerMonitorsComponent },
      { path: 'alerts', component: AlertsComponent }
    ]
  },

  { path: '**', redirectTo: '/monitors/apis' }
];