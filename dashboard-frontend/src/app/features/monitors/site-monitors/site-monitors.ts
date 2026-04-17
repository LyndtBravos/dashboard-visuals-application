import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SiteMonitorService, SiteMonitor } from '../../../core/services/site-monitor';
import { NotificationService } from '../../../core/services/notification';
import { UniversalCrudModalComponent, FieldConfig } from '../../../shared/components/universal-crud-modal/universal-crud-modal';
import { ConfirmService } from '../../../core/services/confirm';

@Component({
  selector: 'app-site-monitors',
  standalone: true,
  imports: [CommonModule, FormsModule, UniversalCrudModalComponent],
  templateUrl: './site-monitors.html',
  styleUrls: ['./site-monitors.css']
})
export class SiteMonitorsComponent implements OnInit {
  monitors: SiteMonitor[] = [];
  filteredMonitors: SiteMonitor[] = [];
  
  showModal = false;
  modalMode: 'create' | 'edit' = 'create';
  modalTitle = '';
  selectedMonitor: SiteMonitor | null = null;
  modalLoading = false;
  isLoading = false;
  
  Math = Math;
  pagination = {
    page: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  };
  
  // Sorting
  sortKey: string = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Search
  searchTerm: string = '';
  
  // Form fields configuration
  formFields: FieldConfig[] = [
    { key: 'name', label: 'Monitor Name', type: 'text', required: true, minLength: 3, maxLength: 200 },
    { key: 'description', label: 'Description', type: 'textarea', required: false, maxLength: 1000 },
    { key: 'url', label: 'URL', type: 'url', required: true, pattern: '^(http|https)://.*$' },
    { key: 'expectedPhrase', label: 'Expected Phrase', type: 'text', required: false },
    { key: 'expectedPhraseMissing', label: 'Should phrase be missing on the page?', type: 'checkbox', required: false, defaultValue: false },
    { key: 'retryCount', label: 'Retry Count', type: 'number', required: true, min: 0, max: 10, defaultValue: 3 },
    { key: 'retryIntervalSeconds', label: 'Retry Interval (seconds)', type: 'number', required: true, min: 5, max: 3600, defaultValue: 60 },
    { key: 'checkIntervalMinutes', label: 'Check Interval (minutes)', type: 'number', required: true, min: 1, max: 1440, defaultValue: 5 },
    { key: 'timeoutSeconds', label: 'Timeout (seconds)', type: 'number', required: true, min: 1, max: 120, defaultValue: 30 },
    { key: 'expectedStatusCode', label: 'Expected Status Code', type: 'number', required: true, min: 100, max: 599, defaultValue: 200 },
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
    { key: 'businessHoursOnly', label: 'Business Hours Only', type: 'checkbox', required: false, defaultValue: false },
    { 
      key: 'businessHoursStart', 
      label: 'Business Hours Start', 
      type: 'time', 
      required: false,
      conditionallyRequired: (formValue) => formValue.businessHoursOnly === true
    },
    { 
      key: 'businessHoursEnd', 
      label: 'Business Hours End', 
      type: 'time', 
      required: false,
      conditionallyRequired: (formValue) => formValue.businessHoursOnly === true
    },
    { 
      key: 'businessDays', 
      label: 'Business Days', 
      type: 'multiselect', 
      required: false,
      options: [
        { value: '1', label: 'Monday' },
        { value: '2', label: 'Tuesday' },
        { value: '3', label: 'Wednesday' },
        { value: '4', label: 'Thursday' },
        { value: '5', label: 'Friday' },
        { value: '6', label: 'Saturday' },
        { value: '7', label: 'Sunday' }
      ],
      defaultValue: ['1', '2', '3', '4', '5'],
      hidden: (formValue) => formValue.businessHoursOnly !== true
    },
    { key: 'alert', label: 'Enable Alerts', type: 'checkbox', required: false, defaultValue: true },
    { 
      key: 'alertEmail', 
      label: 'Alert Email', 
      type: 'email', 
      required: false,
      conditionallyRequired: (formValue) => formValue.alert === true
    }
  ];

  constructor(
    private siteMonitorService: SiteMonitorService,
    private notificationService: NotificationService,
    private confirmService: ConfirmService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadMonitors();
  }

  loadMonitors(): void {
    this.siteMonitorService.getAll().subscribe({
      next: (data: SiteMonitor[]) => {
        this.monitors = data;
        this.applyFilters();
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.cdr.detectChanges();
        this.notificationService.error('Failed to load site monitors: Unknown error');
        console.error('Error loading monitors:', error);
      }
    });
  }

  applyFilters(): void {
    // Filter by search term
    let filtered = [...this.monitors];
    
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(m => 
        m.name.toLowerCase().includes(term) ||
        m.url.toLowerCase().includes(term) ||
        m.serviceType.toLowerCase().includes(term)
      );
    }
    
    // Sort
    filtered.sort((a, b) => {
      let aVal = a[this.sortKey as keyof SiteMonitor];
      let bVal = b[this.sortKey as keyof SiteMonitor];
      
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
    
    this.filteredMonitors = filtered;
    this.pagination.totalItems = filtered.length;
    this.pagination.totalPages = Math.ceil(filtered.length / this.pagination.pageSize);
    this.pagination.page = 1;
  }

  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
    this.cdr.detectChanges();
  }

  onSort(key: string): void {
    if (this.sortKey === key) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    this.applyFilters();
    this.cdr.detectChanges();
  }

  recheckMonitor(monitor: SiteMonitor): void {
    this.notificationService.info(`Rechecking "${monitor.name}"...`);
    
    this.siteMonitorService.recheck(monitor.id!).subscribe({
      next: (updatedMonitor: SiteMonitor) => {
        const index = this.monitors.findIndex(m => m.id === updatedMonitor.id);
        if (index !== -1)
          this.monitors[index] = updatedMonitor;
        
        this.applyFilters();
        this.notificationService.success(`Recheck completed for "${monitor.name}"`);
        this.cdr.detectChanges();
      },
      error: (error: Error) => {
        this.notificationService.error(`Recheck failed for "${monitor.name}": 'Unknown error`);
        console.error('Recheck error:', error);
      }
    });
  }

  refreshAllMonitors(): void {
    this.notificationService.info('Refreshing all monitors...');
    this.isLoading = true;
    
    const failingMonitors = this.monitors.filter(m => m.currentFailureCount && m.currentFailureCount > 0);
    
    if (failingMonitors.length === 0) {
      this.notificationService.info('All monitors are healthy');
      this.loadMonitors(); // Just reload to update timestamps
      return;
    }
    
    let completed = 0;
    failingMonitors.forEach(monitor => {
      this.siteMonitorService.recheck(monitor.id!).subscribe({
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
          completed++;
          console.error(`Failed to recheck ${monitor.name}:`, error);
          if (completed === failingMonitors.length) {
            this.applyFilters();
            this.notificationService.warning(`Refreshed ${completed - 1} of ${failingMonitors.length} monitors`);
            this.cdr.detectChanges();
          }
          this.isLoading = false;
        }
      });
    });
  }

  onCreate(): void {
    this.modalMode = 'create';
    this.modalTitle = 'Create Site Monitor';
    this.selectedMonitor = null;
    this.showModal = true;
  }

  onEdit(monitor: SiteMonitor): void {
    this.modalMode = 'edit';
    this.modalTitle = 'Edit Site Monitor';
    this.selectedMonitor = monitor;
    this.showModal = true;
  }

  onDelete(monitor: SiteMonitor): void {
    this.confirmService.confirmDelete(monitor.name).then(confirmed => {
      if (confirmed) 
        this.siteMonitorService.delete(monitor.id!).subscribe({
          next: () => {
            this.notificationService.success(`Site monitor "${monitor.name}" deleted successfully`);
            this.loadMonitors();
          },
          error: (error: Error) => {
            this.notificationService.error('Failed to delete monitor: Unknown error');
            console.error('Error deleting monitor:', error);
          }
        });
    });
  }

  onSubmit(formData: any): void {
    this.modalLoading = true;
    
    if (formData.businessDays && Array.isArray(formData.businessDays)) {
      formData.businessDays = formData.businessDays.join(',');
    }
    
    if (this.modalMode === 'create') {
      this.siteMonitorService.create(formData).subscribe({
        next: () => {
          this.modalLoading = false;
          this.showModal = false;
          this.notificationService.success('Site monitor created successfully');
          this.loadMonitors();
        },
        error: (error: Error) => {
          this.modalLoading = false;
          this.notificationService.error('Failed to create monitor: Unknown error');
          console.error('Error creating monitor:', error);
        }
      });
    } else {
      const updatedMonitor = { ...formData, id: this.selectedMonitor?.id };
      this.siteMonitorService.update(this.selectedMonitor!.id!, updatedMonitor).subscribe({
        next: () => {
          this.modalLoading = false;
          this.showModal = false;
          this.notificationService.success(`Site monitor "${formData.name}" updated successfully`);
          this.loadMonitors();
        },
        error: (error: Error) => {
          this.modalLoading = false;
          this.notificationService.error('Failed to update monitor: Unknown error');
          console.error('Error updating monitor:', error);
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
    this.cdr.detectChanges();
  }

  onPageSizeChange(pageSize: number): void {
    this.pagination.pageSize = pageSize;
    this.pagination.page = 1;
    this.pagination.totalPages = Math.ceil(this.filteredMonitors.length / pageSize);
    this.cdr.detectChanges();
  }

  getPaginatedMonitors(): SiteMonitor[] {
    const start = (this.pagination.page - 1) * this.pagination.pageSize;
    const end = start + this.pagination.pageSize;
    return this.filteredMonitors.slice(start, end);
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