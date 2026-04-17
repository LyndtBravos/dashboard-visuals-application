import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertDetailModalComponent } from '../alert-detail-modal/alert-detail-modal';
import { AlertService, Alert, AlertCounts, AlertDetail } from '../../../core/services/alert';
import { ConfirmService } from '../../../core/services/confirm';
import { NotificationService } from '../../../core/services/notification';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule, AlertDetailModalComponent],
  templateUrl: './alerts.html',
  styleUrls: ['./alerts.css']
})
export class AlertsComponent implements OnInit, OnDestroy {
  alerts: Alert[] = [];
  filteredAlerts: Alert[] = [];
  showAcknowledged = false;
  alertCounts: AlertCounts = { total: 0, critical: 0, high: 0, medium: 0, low: 0 };

  selectedAlertDetail: AlertDetail | null = null;
  showDetailModal = false;
  
  autoRefreshEnabled = false;
  autoRefreshInterval: any = null;
  refreshOptions = [
    { value: 0, label: 'Off' },
    { value: 10, label: 'Every 10 seconds' },
    { value: 30, label: 'Every 30 seconds' },
    { value: 60, label: 'Every minute' },
    { value: 300, label: 'Every 5 minutes' }
  ];
  selectedRefreshInterval = 30;
  
  Math = Math;
  pagination = {
    page: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  };
  
  sortKey: string = 'severity';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  constructor(
    private alertService: AlertService,
    private notificationService: NotificationService,
    private confirmService: ConfirmService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadAlerts();
    this.loadAlertCounts();
  }

  ngOnDestroy(): void {
    this.stopAutoRefresh();
  }

  viewAlertDetail(alert: Alert): void {
    this.alertService.getAlertDetail(alert.id).subscribe({
      next: (detail) => {
        this.selectedAlertDetail = detail;
        this.showDetailModal = true;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.notificationService.error('Failed to load alert details');
        console.error('Error loading alert details:', error);
      }
    });
  }

  loadAlerts(): void {
    this.alertService.getAllAlerts(this.showAcknowledged).subscribe({
      next: (data: Alert[]) => {
        this.alerts = data;
        this.applyFilterAndSort();
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.cdr.detectChanges();
        this.notificationService.error('Failed to load alerts:  Unknown error');
        console.error('Error loading alerts:', error);
      }
    });
  }

  loadAlertCounts(): void {
    this.alertService.getAlertCounts().subscribe({
      next: (data: any) => {
        this.alertCounts = {
          total: data.total || 0,
          critical: data.critical || 0,
          high: data.high || 0,
          medium: data.medium || 0,
          low: data.low || 0
        };
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        console.error('Error loading alert counts:', error);
      }
    });
  }

  applyFilterAndSort(): void {
    // Filter
    this.filteredAlerts = this.showAcknowledged 
      ? [...this.alerts] 
      : this.alerts.filter(alert => !alert.acknowledged);
    
    // Sort
    this.filteredAlerts.sort((a, b) => {
      let aVal = a[this.sortKey as keyof Alert];
      let bVal = b[this.sortKey as keyof Alert];
      
      if (this.sortKey === 'severity') {
        const severityOrder = { critical: 4, high: 3, medium: 2, low: 1 };
        aVal = severityOrder[a.severity as keyof typeof severityOrder];
        bVal = severityOrder[b.severity as keyof typeof severityOrder];
      }
      
      if (aVal === undefined || bVal === undefined) return 0;
      
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return this.sortDirection === 'asc' 
          ? aVal.localeCompare(bVal) 
          : bVal.localeCompare(aVal);
      }
      
      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
    
    this.pagination.totalItems = this.filteredAlerts.length;
    this.pagination.totalPages = Math.ceil(this.filteredAlerts.length / this.pagination.pageSize);
  }

  toggleShowAcknowledged(): void {
    this.showAcknowledged = !this.showAcknowledged;
    this.pagination.page = 1;
    this.loadAlerts();
  }

  onAcknowledge(alert: Alert): void {
    this.alertService.acknowledgeAlert(alert.id).subscribe({
      next: () => {
        this.notificationService.success(`Alert "${alert.name}" acknowledged`);
        this.loadAlerts();
        this.loadAlertCounts();
      },
      error: (error: Error) => {
        this.notificationService.error('Failed to acknowledge alert: Unknown error');
        console.error('Error acknowledging alert:', error);
      }
    });
  }

  onResolve(alert: Alert): void {
    this.confirmService.confirmResolve(alert.name).then(confirmed => {
      if (confirmed) {
        this.alertService.resolveAlert(alert.id).subscribe({
          next: () => {
            this.notificationService.success(`Alert "${alert.name}" resolved`);
            this.loadAlerts();
            this.loadAlertCounts();
          },
          error: (error: Error) => {
            this.notificationService.error('Failed to resolve alert: Unknown error');
            console.error('Error resolving alert:', error);
          }
        });
      }
    });
  }

  onRefresh(): void {
    this.loadAlerts();
    this.loadAlertCounts();
  }

  onAutoRefreshChange(): void {
    this.stopAutoRefresh();
    if (this.selectedRefreshInterval > 0) {
      this.startAutoRefresh();
      this.notificationService.info(`Auto-refresh enabled: every ${this.selectedRefreshInterval} seconds`);
    } else 
      this.notificationService.info('Auto-refresh disabled');
  }

  startAutoRefresh(): void {
    this.autoRefreshInterval = setInterval(() => {
      this.loadAlerts();
      this.loadAlertCounts();
    }, this.selectedRefreshInterval * 1000);
  }

  stopAutoRefresh(): void {
    if (this.autoRefreshInterval) {
      clearInterval(this.autoRefreshInterval);
      this.autoRefreshInterval = null;
    }
  }

  onSort(key: string): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.applyFilterAndSort();
    this.cdr.detectChanges();
  }

  onPageChange(page: number): void {
    this.pagination.page = page;
    this.cdr.detectChanges();
  }

  onPageSizeChange(pageSize: number): void {
    this.pagination.pageSize = pageSize;
    this.pagination.page = 1;
    this.pagination.totalPages = Math.ceil(this.filteredAlerts.length / pageSize);
    this.cdr.detectChanges();
  }

  getPaginatedAlerts(): Alert[] {
    const start = (this.pagination.page - 1) * this.pagination.pageSize;
    const end = start + this.pagination.pageSize;
    return this.filteredAlerts.slice(start, end);
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredAlerts.length / this.pagination.pageSize);
  }

  getPages(): number[] {
    const total = this.getTotalPages();
    const current = this.pagination.page;
    const pages: number[] = [];
    
    if (total <= 7) {
      for (let i = 1; i <= total; i++) pages.push(i);
    } else {
      if (current <= 4) {
        for (let i = 1; i <= 5; i++) pages.push(i);
        pages.push(-1);
        pages.push(total);
      } else if (current >= total - 3) {
        pages.push(1);
        pages.push(-1);
        for (let i = total - 4; i <= total; i++) pages.push(i);
      } else {
        pages.push(1);
        pages.push(-1);
        for (let i = current - 1; i <= current + 1; i++) pages.push(i);
        pages.push(-1);
        pages.push(total);
      }
    }
    return pages;
  }

  getSeverityClass(severity: string): string {
    switch (severity) {
      case 'critical': return 'severity-critical';
      case 'high': return 'severity-high';
      case 'medium': return 'severity-medium';
      case 'low': return 'severity-low';
      default: return '';
    }
  }

  getSeverityIcon(severity: string): string {
    switch (severity) {
      case 'critical': return '🔴';
      case 'high': return '🟠';
      case 'medium': return '🟡';
      case 'low': return '🔵';
      default: return '⚪';
    }
  }

  getMonitorTypeIcon(type: string): string {
    switch (type) {
      case 'site': return '🌐';
      case 'api': return '🔌';
      case 'server': return '🖥️';
      default: return '❓';
    }
  }
}