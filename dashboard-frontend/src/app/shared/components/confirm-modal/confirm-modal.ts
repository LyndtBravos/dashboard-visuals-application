import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ConfirmService, ConfirmOptions } from '../../../core/services/confirm';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './confirm-modal.html',
  styleUrls: ['./confirm-modal.css']
})
export class ConfirmModalComponent implements OnInit, OnDestroy {
  isVisible = false;
  options: ConfirmOptions = {
    title: 'Confirm',
    message: 'Are you sure?',
    confirmText: 'OK',
    cancelText: 'Cancel',
    confirmButtonClass: 'primary',
    type: 'info'
  };
  
  private resolveFn!: (value: boolean) => void;
  private subscription!: Subscription;
  
  constructor(private confirmService: ConfirmService) {}
  
  ngOnInit(): void {
    this.subscription = this.confirmService.confirm$.subscribe(({ options, resolve }) => {
      this.options = { ...this.options, ...options };
      this.resolveFn = resolve;
      this.isVisible = true;
    });
  }
  
  ngOnDestroy(): void {
    if (this.subscription) this.subscription.unsubscribe();    
  }
  
  onConfirm(): void {
    this.isVisible = false;
    this.resolveFn(true);
  }
  
  onCancel(): void {
    this.isVisible = false;
    this.resolveFn(false);
  }
  
  getHeaderClass(): string {
    switch (this.options.type) {
      case 'danger': return 'modal-header-danger';
      case 'warning': return 'modal-header-warning';
      case 'success': return 'modal-header-success';
      default: return 'modal-header-info';
    }
  }
  
  getConfirmButtonClass(): string {
    switch (this.options.confirmButtonClass) {
      case 'danger': return 'btn-danger';
      case 'success': return 'btn-success';
      case 'primary': return 'btn-primary';
      default: return 'btn-primary';
    }
  }
  
  getIcon(): string {
    switch (this.options.type) {
      case 'danger': return '❌';
      case 'warning': return '⚠️';
      case 'success': return '✅';
      default: return 'ℹ️';
    }
  }
}