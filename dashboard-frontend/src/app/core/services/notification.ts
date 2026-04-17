import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface Notification {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationsSubject = new Subject<Notification>();
  private idCounter = 0;
  
  notifications$ = this.notificationsSubject.asObservable();
  
  success(message: string, duration: number = 5000): void {
    this.show({ type: 'success', message, duration });
  }
  
  error(message: string, duration: number = 5000): void {
    console.error('Notification - Error:', message);
    this.show({ type: 'error', message, duration });
  }
  
  warning(message: string, duration: number = 5000): void {
    console.warn('Notification - Warning:', message);
    this.show({ type: 'warning', message, duration });
  }
  
  info(message: string, duration: number = 5000): void {
    console.info('Notification - Info:', message);
    this.show({ type: 'info', message, duration });
  }
  
  private show(notification: Omit<Notification, 'id'>): void {
    const id = ++this.idCounter;
    this.notificationsSubject.next({ ...notification, id });
    
    if (notification.duration && notification.duration > 0) {
      setTimeout(() => {
        this.clear(id);
      }, notification.duration);
    }
  }
  
  clear(id: number): void {
    this.notificationsSubject.next({ id, type: 'success', message: '', duration: 0 });
  }
}