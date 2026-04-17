import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

export interface ConfirmOptions {
  title?: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  confirmButtonClass?: string;
  type?: 'danger' | 'warning' | 'info' | 'success';
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmService {
  private confirmSubject = new Subject<{ options: ConfirmOptions; resolve: (value: boolean) => void }>();
  
  confirm$ = this.confirmSubject.asObservable();
  
  show(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.confirmSubject.next({ options, resolve });
    });
  }
  
  confirmDelete(itemName: string): Promise<boolean> {
    return this.show({
      title: 'Confirm Delete',
      message: `Are you sure you want to delete "${itemName}"? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmButtonClass: 'danger',
      type: 'danger'
    });
  }
  
  confirmResolve(itemName: string): Promise<boolean> {
    return this.show({
      title: 'Confirm Resolve',
      message: `Mark "${itemName}" as resolved? This alert will no longer appear in active alerts.`,
      confirmText: 'Resolve',
      cancelText: 'Cancel',
      confirmButtonClass: 'success',
      type: 'info'
    });
  }
  
  confirmAcknowledge(itemName: string): Promise<boolean> {
    return this.show({
      title: 'Confirm Acknowledge',
      message: `Acknowledge "${itemName}"? This will mark the alert as acknowledged.`,
      confirmText: 'Acknowledge',
      cancelText: 'Cancel',
      confirmButtonClass: 'primary',
      type: 'info'
    });
  }
}