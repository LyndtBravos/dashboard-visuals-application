// layouts/header/header.component.ts
import { Component, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../../app/core/auth/auth';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class HeaderComponent {
  @Output() toggleSidebar = new EventEmitter<void>();
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }
  
  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
  
  getUserName(): string {
    const user = this.authService.getUser();
    return user?.name || 'User';
  }
}