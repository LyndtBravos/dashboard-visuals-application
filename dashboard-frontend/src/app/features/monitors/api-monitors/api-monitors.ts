import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiMonitorService, ApiMonitor } from '../../../core/services/api-monitor';
import { NotificationService } from '../../../core/services/notification';
import { ConfirmService } from '../../../core/services/confirm';
import { UniversalCrudModalComponent, FieldConfig } from '../../../shared/components/universal-crud-modal/universal-crud-modal';

@Component({
  selector: 'app-api-monitors',
  standalone: true,
  imports: [CommonModule, FormsModule, UniversalCrudModalComponent],
  templateUrl: './api-monitors.html',
  styleUrls: ['./api-monitors.css']
})
export class ApiMonitorsComponent implements OnInit {
  monitors: ApiMonitor[] = [];
  filteredMonitors: ApiMonitor[] = [];
  
  showModal = false;
  modalMode: 'create' | 'edit' = 'create';
  modalTitle = '';
  selectedMonitor: ApiMonitor | null = null;
  modalLoading = false;
  isLoading = false;
  
  Math = Math;
  pagination = {
    page: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  };
  
  sortKey: string = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  searchTerm: string = '';
  
  formFields: FieldConfig[] = [
    { key: 'name', label: 'API Name', type: 'text', required: true, minLength: 3, maxLength: 200 },
    { key: 'description', label: 'Description', type: 'textarea', required: false, maxLength: 1000 },
    { key: 'url', label: 'URL', type: 'url', required: true, pattern: '^(http|https)://.*$' },
    { 
      key: 'method', 
      label: 'HTTP Method', 
      type: 'select', 
      required: true, 
      options: [
        { value: 'GET', label: 'GET' },
        { value: 'POST', label: 'POST' },
        { value: 'PUT', label: 'PUT' },
        { value: 'DELETE', label: 'DELETE' }
      ],
      defaultValue: 'GET'
    },
    { key: 'requestHeadersJson', label: 'Request Headers (JSON)', type: 'textarea', required: false },
    { key: 'requestBody', label: 'Request Body', type: 'textarea', required: false },
    { key: 'expectedStatusCode', label: 'Expected Status Code', type: 'number', required: true, defaultValue: 200 },
    { key: 'expectedResponseTimeMs', label: 'Max Response Time (ms)', type: 'number', required: false },
    { key: 'expectedResponseContains', label: 'Expected Response Text', type: 'text', required: false },
    { key: 'timeoutSeconds', label: 'Timeout (seconds)', type: 'number', required: true, defaultValue: 30 },
    { key: 'retryCount', label: 'Retry Count', type: 'number', required: true, defaultValue: 3 },
    { key: 'checkIntervalMinutes', label: 'Check Interval (minutes)', type: 'number', required: true, defaultValue: 5 },
    { 
      key: 'serviceType', 
      label: 'Service Type', 
      type: 'select', 
      required: true, 
      options: [
        { value: 'Broadcast', label: 'Broadcast' },
        { value: 'Print', label: 'Print' },
        { value: 'Online', label: 'Online' }
      ],
      defaultValue: 'Broadcast'
    },
    { 
      key: 'severity', 
      label: 'Severity', 
      type: 'select', 
      required: true, 
      options: [
        { value: 'critical', label: 'Critical' },
        { value: 'high', label: 'High' },
        { value: 'medium', label: 'Medium' },
        { value: 'low', label: 'Low' }
      ],
      defaultValue: 'medium'
    },
    { key: 'alert', label: 'Enable Alerts', type: 'checkbox', required: false, defaultValue: true },
    { key: 'alertEmail', label: 'Alert Email', type: 'email', required: false }
  ];

  constructor(
    private apiMonitorService: ApiMonitorService,
    private notificationService: NotificationService,
    private confirmService: ConfirmService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadMonitors();
  }

  loadMonitors(): void {
    this.apiMonitorService.getAll().subscribe({
      next: (data) => {
        this.isLoading = false;
        this.monitors = data;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: () => {
        this.isLoading = false;
        this.cdr.detectChanges();
        this.notificationService.error('Failed to load API monitors: Unknown error');
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.monitors];
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(m => 
        m.name.toLowerCase().includes(term) ||
        m.url.toLowerCase().includes(term) ||
        m.method.toLowerCase().includes(term)
      );
    }
    
    filtered.sort((a, b) => {
      let aVal = a[this.sortKey as keyof ApiMonitor];
      let bVal = b[this.sortKey as keyof ApiMonitor];
      if (aVal === undefined || bVal === undefined) return 0;
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return this.sortDirection === 'asc' ? aVal.localeCompare(bVal) : bVal.localeCompare(aVal);
      }
      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
    
    this.filteredMonitors = filtered;
    this.pagination.totalItems = filtered.length;
    this.pagination.totalPages = Math.ceil(filtered.length / this.pagination.pageSize);
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
  }

  onSort(key: string): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
  }

  recheckMonitor(monitor: ApiMonitor): void {
    this.notificationService.info(`Rechecking "${monitor.name}"...`);
    this.apiMonitorService.recheck(monitor.id!).subscribe({
      next: (updated) => {
        const index = this.monitors.findIndex(m => m.id === updated.id);
        if (index !== -1) this.monitors[index] = updated;
        this.applyFilters();
        this.notificationService.success(`Recheck completed for "${monitor.name}"`);
      },
      error: (error) => {
        this.notificationService.error(`Recheck failed: Uknown error`);
        console.error("Error: ", error.message);
      }
    });
  }

  refreshAllMonitors(): void {
    this.isLoading = true;
    this.notificationService.info('Refreshing all monitors...');
    
    const failingMonitors = this.monitors.filter(m => m.currentFailureCount && m.currentFailureCount > 0);
    
    if (failingMonitors.length === 0) {
      this.notificationService.info('All monitors are healthy');
      this.loadMonitors();
      this.isLoading = false;
return;
    }
    
    let completed = 0;
    failingMonitors.forEach(monitor => {
      this.apiMonitorService.recheck(monitor.id!).subscribe({
        next: (updated) => {
          this.isLoading = false;
          const index = this.monitors.findIndex(m => m.id === updated.id);
          if (index !== -1) this.monitors[index] = updated;
          completed++;
          
          if (completed === failingMonitors.length) {
            this.applyFilters();
            this.notificationService.success(`Refreshed ${completed} monitor(s)`);
            this.cdr.detectChanges();
          }
        },
        error: (error) => {
          this.isLoading = false;
          completed++;
          console.error(`Failed to recheck ${monitor.name}:`, error);
          if (completed === failingMonitors.length) {
            this.applyFilters();
            this.notificationService.warning(`Refreshed ${completed - 1} of ${failingMonitors.length} monitors`);
            this.cdr.detectChanges();
          }
        }
      });
    });
  }

  onCreate(): void {
    this.modalMode = 'create';
    this.modalTitle = 'Create API Monitor';
    this.selectedMonitor = null;
    this.showModal = true;
  }

  onEdit(monitor: ApiMonitor): void {
    this.modalMode = 'edit';
    this.modalTitle = 'Edit API Monitor';
    this.selectedMonitor = monitor;
    this.showModal = true;
  }

  onDelete(monitor: ApiMonitor): void {
    this.confirmService.confirmDelete(monitor.name).then(confirmed => {
      if (confirmed)
        this.apiMonitorService.delete(monitor.id!).subscribe({
          next: () => {
            this.notificationService.success(`API monitor "${monitor.name}" deleted`);
            this.loadMonitors();
          },
          error: (error) => {
            this.notificationService.error('Delete failed: Unknown error');
            console.log("Error: ", error.message);
          }
        });
    });
  }

  onSubmit(formData: any): void {
    this.modalLoading = true;
    if (this.modalMode === 'create') {
      this.apiMonitorService.create(formData).subscribe({
        next: () => {
          this.modalLoading = false;
          this.showModal = false;
          this.notificationService.success('API monitor created');
          this.loadMonitors();
        },
        error: (error) => {
          this.modalLoading = false;
          this.notificationService.error('Create failed: Unknown error');
          console.log("Error: ", error.message);
        }
      });
    } else {
      const updated = { ...formData, id: this.selectedMonitor?.id };
      this.apiMonitorService.update(this.selectedMonitor!.id!, updated).subscribe({
        next: () => {
          this.modalLoading = false;
          this.showModal = false;
          this.notificationService.success('API monitor updated');
          this.loadMonitors();
        },
        error: (error) => {
          this.modalLoading = false;
          this.notificationService.error('Update failed: Unknown error');
          console.error("Error: ", error.message);
        }
      });
    }
  }

  onCloseModal(): void {
    this.showModal = false;
    this.selectedMonitor = null;
    this.modalLoading = false;
  }

  onPageChange(page: number): void {
    this.pagination.page = page;
  }

  onPageSizeChange(pageSize: number): void {
    this.pagination.pageSize = pageSize;
    this.pagination.page = 1;
  }

  getPaginatedMonitors(): ApiMonitor[] {
    const start = (this.pagination.page - 1) * this.pagination.pageSize;
    return this.filteredMonitors.slice(start, start + this.pagination.pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredMonitors.length / this.pagination.pageSize);
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

  getStatusClass(status: string): string {
    switch (status) {
      case 'success': return 'status-success';
      case 'failed': return 'status-failed';
      case 'error': return 'status-error';
      default: return 'status-pending';
    }
  }
}