import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.html',
  styleUrls: ['./sidebar.css']
})
export class SidebarComponent {
  @Input() isCollapsed = false;
  @Output() closeSidebar = new EventEmitter<void>();
  
  menuItems = [
    { path: '/dashboard', icon: '📊', label: 'Dashboard' },
    { path: '/monitors/apis', icon: '🔌', label: 'API Monitors' },
    { path: '/monitors/sites', icon: '🌐', label: 'Site Monitors' },
    { path: '/monitors/servers', icon: '🖥️', label: 'Server Monitors' },
    { path: '/alerts', icon: '⚠️', label: 'Alerts' }
  ];
  
  onClose(): void {
    this.closeSidebar.emit();
  }
}