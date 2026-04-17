import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, Notification } from '../../../core/services/notification';
import { Subject, takeUntil } from 'rxjs';

@Component({
  selector: 'app-notification-toast',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-toast.html',
  styleUrls: ['./notification-toast.css']
})
export class NotificationToastComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  private destroy$ = new Subject<void>();
  
  constructor(private notificationService: NotificationService) {}
  
  ngOnInit(): void {
    this.notificationService.notifications$
      .pipe(takeUntil(this.destroy$))
      .subscribe(notification => {
        if (notification.message) {
          this.notifications.push(notification);
          setTimeout(() => this.removeNotification(notification.id), notification.duration || 5000);
        } else {
          this.notifications = this.notifications.filter(n => n.id !== notification.id);
        }
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  removeNotification(id: number): void {
    this.notifications = this.notifications.filter(n => n.id !== id);
  }
  
  getIcon(type: string): string {
    switch (type) {
      case 'success': return '✓';
      case 'error': return '✗';
      case 'warning': return '⚠';
      case 'info': return 'ℹ';
      default: return '';
    }
  }
  
  getClass(type: string): string {
    return `toast-${type}`;
  }
}