import { Component, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ViewChildren, QueryList } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { VisualCardComponent } from './components/visual-card/visual-card.component';
import { DashboardService, ServiceSummary, DashboardConfig } from '../../../core/services/dashboard-overview';
import { NotificationService } from '../../../core/services/notification';
import { PollingService, PollingConfig } from '../../../core/services/polling';
import { ConfirmService } from '../../../core/services/confirm';
import { VisualBuilderComponent } from '../visual-builder/visual-builder';
import { FlowDiagramComponent } from '../flow-diagram/flow-diagram';

import { HttpErrorResponse } from '@angular/common/http';

interface DisplayServiceSummary {
  serviceType: string;
  totalVisuals: number;
  statusCounts: {
    green: number;
    yellow: number;
    red: number;
  };
  overallStatus: 'green' | 'yellow' | 'red';
}

@Component({
  selector: 'app-service-detail',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterModule, 
    VisualCardComponent, 
    VisualBuilderComponent, 
    FlowDiagramComponent
  ],
  templateUrl: './service-detail.component.html',
  styleUrls: ['./service-detail.component.css']
})
export class ServiceDetailComponent implements OnInit, OnDestroy {
  
  @ViewChild('flowDiagram') flowDiagramComponent!: FlowDiagramComponent;
  @ViewChildren(VisualCardComponent) visualCardComponents!: QueryList<VisualCardComponent>;
  
  serviceType: string | null = null;
  services: DisplayServiceSummary[] = [];
  visuals: DashboardConfig[] = [];
  isLoading = false;
  lastUpdated: Date | null = null;
  useFlowDiagram: boolean = true;

  showShareModal = false;
  isSendingEmail = false;
  emailError = '';

  emailReport = {
    to: '',
    subject: 'Dashboard Report',
    message: '',
    includeGreen: true,
    includeYellow: true,
    includeRed: true,
    includePdf: false,
    serviceType: ''
  };
  
  pollingConfig: PollingConfig = { enabled: false, intervalMs: 30000, lastRun: null };
  availableIntervals = [
    { value: 5000, label: '5 seconds', seconds: 5 },
    { value: 10000, label: '10 seconds', seconds: 10 },
    { value: 15000, label: '15 seconds', seconds: 15 },
    { value: 30000, label: '30 seconds', seconds: 30 },
    { value: 60000, label: '1 minute', seconds: 60 },
    { value: 120000, label: '2 minutes', seconds: 120 },
    { value: 300000, label: '5 minutes', seconds: 300 }
  ];
  selectedInterval = 30000;
  showPollingSettings = false;
  
  searchTerm: string = '';
  filteredVisuals: DashboardConfig[] = [];

  modalSortKey: string = 'name';
  modalSortDirection: 'asc' | 'desc' = 'asc';
  
  showVisualsModal = false;
  showVisualBuilder = false;
  selectedVisual: DashboardConfig | null = null;
  
  totalVisuals = 0;
  healthyCount = 0;
  warningCount = 0;
  criticalCount = 0;
  
  visualColumns = [
    { key: 'name', label: 'Name', sortable: true },
    { key: 'graphType', label: 'Type', sortable: true },
    { key: 'currentValue', label: 'Value', sortable: true },
    { key: 'currentStatus', label: 'Status', sortable: true },
    { key: 'lastChecked', label: 'Last Check', sortable: true }
  ];
  
  visualPagination = {
    page: 1,
    pageSize: 10,
    totalItems: 0,
    totalPages: 0
  };
  
  sortKey: string = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';

  constructor(
    private route: ActivatedRoute,
    private dashboardService: DashboardService,
    private notificationService: NotificationService,
    private pollingService: PollingService,
    private confirmService: ConfirmService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.params.subscribe(params => {
      this.serviceType = params['type'] || null;
      this.loadData();
    });
    
    this.pollingService.pollingStatus$.subscribe(config => {
      this.pollingConfig = config;
      if (config.lastRun)
        this.lastUpdated = config.lastRun;
      
      this.cdr.detectChanges();
    });
  }

  ngOnDestroy(): void {
    this.pollingService.stopPolling();
  }

  onSort(key: string): void {
    if (this.sortKey === key)
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    else {
      this.sortKey = key;
      this.sortDirection = 'asc';
    }
    
    // Sort the visuals array
    this.visuals.sort((a, b) => {
      let aVal = a[key as keyof DashboardConfig];
      let bVal = b[key as keyof DashboardConfig];
      
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
    
    this.cdr.detectChanges();
  }

  filterVisuals(): void {
    if (!this.searchTerm || this.searchTerm.trim() === '') {
      this.filteredVisuals = [...this.visuals];
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredVisuals = this.visuals.filter(visual => 
        visual.name.toLowerCase().includes(term) ||
        visual.description?.toLowerCase().includes(term) ||
        visual.graphType?.toLowerCase().includes(term)
      );
    }

    this.visualPagination.page = 1;
    this.visualPagination.totalItems = this.filteredVisuals.length;
    this.visualPagination.totalPages = Math.ceil(this.filteredVisuals.length / this.visualPagination.pageSize);
    this.cdr.detectChanges();
  }

  loadServiceVisuals(): void {
    if (!this.serviceType) return;
    
    this.isLoading = true;
    this.dashboardService.getOrderedDashboardsByService(this.serviceType).subscribe({
      next: (data: DashboardConfig[]) => {
        this.visuals = data;
        this.filteredVisuals = [...data];
        this.calculateStats();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.isLoading = false;
        this.cdr.detectChanges();
        this.notificationService.error('Failed to load visuals: Unknown error');
        console.error('Error loading visuals:', error);
      }
    });
  }

  get hasFlowVisuals(): boolean {
    return this.visuals.some(v => v.flowOrder !== null && v.flowOrder !== undefined && 
          ['text', 'text_minutes', 'status_indicator'].includes(v.graphType));
  }

  get hasCharts(): boolean {
    return this.visuals.some(v => v.graphType === 'bar' || v.graphType === 'time_series');
  }

  loadData(): void {
    if (this.serviceType)
      this.loadServiceVisuals();
    else
      this.loadServiceSummaries();
  }

  loadServiceSummaries(): void {
    this.isLoading = true;
    this.dashboardService.getAllServicesSummary().subscribe({
      next: (data: ServiceSummary[]) => {
        this.services = data.map(service => ({
          serviceType: service.serviceType,
          totalVisuals: service.totalVisuals,
          statusCounts: {
            green: service.statusCounts.green,
            yellow: service.statusCounts.yellow,
            red: service.statusCounts.red
          },
          overallStatus: this.getOverallStatus({
            green: service.statusCounts.green,
            yellow: service.statusCounts.yellow,
            red: service.statusCounts.red
          })
        }));
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        this.isLoading = false;
        this.cdr.detectChanges();
        this.notificationService.error('Failed to load services: Unknown error');
        console.error('Error loading services:', error);
      }
    });
  }

  calculateStats(): void {
    const activeVisuals = this.visuals.filter(v => v.isActive);
    this.totalVisuals = activeVisuals.length;
    this.healthyCount = activeVisuals.filter(v => v.currentStatus === 'green').length;
    this.warningCount = activeVisuals.filter(v => v.currentStatus === 'yellow').length;
    this.criticalCount = activeVisuals.filter(v => v.currentStatus === 'red').length;
  }

  getOverallStatus(statusCounts: { green: number; yellow: number; red: number }): 'green' | 'yellow' | 'red' {
    if (statusCounts.red > 0) return 'red';
    if (statusCounts.yellow > 0) return 'yellow';
    return 'green';
  }

  getServiceIcon(serviceType: string): string {
    switch (serviceType) {
      case 'Broadcast': return '📡';
      case 'Print': return '🖨️';
      case 'Online': return '🌐';
      case 'Alerts': return '⚠️';
      case 'Other': return '🎲';
      default: return '🚚';
    }
  }

  getServiceColor(serviceType: string): string {
    switch (serviceType) {
      case 'Broadcast': return 'broadcast';
      case 'Print': return 'print';
      case 'Online': return 'online';
      case 'Alerts': return 'alerts';
      case 'Other': return 'other';
      default: return '';
    }
  }
  
  onPollingToggle(): void {
    if (this.pollingConfig.enabled) {
      this.pollingService.stopPolling();
      this.notificationService.info('Auto-refresh disabled');
    } else {
      this.pollingService.startPolling(() => this.loadData(), this.selectedInterval);
      this.notificationService.info(`Auto-refresh enabled: every ${this.selectedInterval / 1000} seconds`);
    }
  }

  onIntervalChange(): void {
    if (this.pollingConfig.enabled) {
      this.pollingService.changeInterval(this.selectedInterval);
      this.notificationService.info(`Auto-refresh interval changed to ${this.selectedInterval / 1000} seconds`);
    }
  }

  manualRefresh(): void {
    this.notificationService.info('Refreshing data...');
    
    if (this.serviceType) {
      this.loadServiceVisuals();
      // Force re-render of flow diagram by resetting and reloading
      setTimeout(() => {
        if (this.flowDiagramComponent) {
          this.flowDiagramComponent.arrangeRows();
          this.flowDiagramComponent.drawArrows();
        }
      }, 100);
    } else 
      this.loadServiceSummaries();
        
    this.lastUpdated = new Date();
    this.notificationService.success('Refresh completed');
  }
  
  openVisualsTable(): void {
    this.showVisualsModal = true;
  }

  editVisual(visual: DashboardConfig): void {
    this.selectedVisual = visual;
    this.showVisualBuilder = true;
  }

  deleteVisual(id: number): void {
    this.confirmService.confirmDelete('this visual').then(confirmed => {
      if (confirmed) 
        this.dashboardService.deleteDashboard(id).subscribe({
          next: () => {
            this.notificationService.success('Visual deleted successfully');
            this.loadServiceVisuals();
        },
        error: (error) => {
          this.notificationService.error('Failed to delete visual');
          console.error('Error deleting visual:', error);
        }
      });
    });
  }

  onVisualBuilderClosed(refresh: boolean = false): void {
    this.showVisualBuilder = false;
    this.selectedVisual = null;
    if (refresh) {
      this.loadServiceVisuals();
    }
  }

  getPaginatedVisuals(): DashboardConfig[] {
    const start = (this.visualPagination.page - 1) * this.visualPagination.pageSize;
    const end = start + this.visualPagination.pageSize;
    return this.filteredVisuals.slice(start, end);
  }

  onPageChange(page: number): void {
    this.visualPagination.page = page;
  }

  onPageSizeChange(pageSize: number): void {
    this.visualPagination.pageSize = pageSize;
    this.visualPagination.page = 1;
    this.visualPagination.totalPages = Math.ceil(this.visuals.length / pageSize);
  }

  getTotalPages(): number {
    return Math.ceil(this.filteredVisuals.length / this.visualPagination.pageSize);
  }
  
  recheckVisual(visual: DashboardConfig): void {
    this.notificationService.info(`Rechecking "${visual.name}"...`);
    
    const visualCardComponent = this.visualCardComponents?.find(card => card.visual.id === visual.id);
    
    if (visualCardComponent) {
      visualCardComponent.refreshData();
      this.notificationService.success(`Recheck completed for "${visual.name}"`);
    } else {
      this.loadServiceVisuals();
      this.notificationService.success(`Recheck completed for "${visual.name}"`);
    }
  }

  openShareModal(): void {
    this.emailReport = {
      to: '',
      subject: 'Dashboard Report',
      message: '',
      includeGreen: true,
      includeYellow: true,
      includeRed: true,
      includePdf: false,
      serviceType: this.serviceType || ''
    };
    this.emailError = '';
    this.showShareModal = true;
  }

  private calculateStatusFromValue(value: number, warning: number, danger: number): 'green' | 'yellow' | 'red' {
    if (danger && value >= danger) return 'red';
    if (warning && value >= warning) return 'yellow';
    return 'green';
  }

  getTextVisuals(): DashboardConfig[] {
    return this.filteredVisuals.filter(v => 
      v.graphType === 'text' || v.graphType === 'text_minutes' || v.graphType === 'status_indicator'
    );
  }

  getChartVisuals(): DashboardConfig[] {
    return this.filteredVisuals.filter(v => 
      v.graphType === 'bar' || v.graphType === 'time_series'
    );
  }

  getPaginatedTextVisuals(): DashboardConfig[] {
    return this.getTextVisuals();
  }

  getPaginatedChartVisuals(): DashboardConfig[] {
    return this.getChartVisuals();
  }

  onModalSort(key: string): void {
    if (this.modalSortKey === key) 
      this.modalSortDirection = this.modalSortDirection === 'asc' ? 'desc' : 'asc';
    else {
      this.modalSortKey = key;
      this.modalSortDirection = 'asc';
    }
    
    this.filteredVisuals.sort((a, b) => {
      let aVal = a[key as keyof DashboardConfig];
      let bVal = b[key as keyof DashboardConfig];
      
      if (aVal === undefined || bVal === undefined) return 0;
      
      if (typeof aVal === 'string' && typeof bVal === 'string') {
        return this.modalSortDirection === 'asc' 
          ? aVal.localeCompare(bVal) 
          : bVal.localeCompare(aVal);
      }
      
      if (aVal < bVal) return this.modalSortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.modalSortDirection === 'asc' ? 1 : -1;
      return 0;
    });
    
    this.visualPagination.page = 1;
    this.cdr.detectChanges();
  }

  sendEmailReport(): void {
    const statusFilters = [];
    if (this.emailReport.includeGreen) statusFilters.push('green');
    if (this.emailReport.includeYellow) statusFilters.push('yellow');
    if (this.emailReport.includeRed) statusFilters.push('red');
    
    if (statusFilters.length === 0) {
      this.emailError = 'Please select at least one status filter';
      return;
    }
    
    this.isSendingEmail = true;
    this.emailError = '';
    
    const payload = {
      to: this.emailReport.to,
      subject: this.emailReport.subject,
      message: this.emailReport.message,
      statusFilters: statusFilters,
      includePdf: this.emailReport.includePdf,
      serviceType: this.emailReport.serviceType || null
    };
    
    this.dashboardService.sendEmailReport(payload).subscribe({
      next: () => {
        this.isSendingEmail = false;
        this.showShareModal = false;
        this.notificationService.success(`Report sent successfully to ${this.emailReport.to}`);
      },
      error: (error: HttpErrorResponse) => {
        this.isSendingEmail = false;
        this.emailError = error.error?.message || 'Failed to send email. Please try again.';
        this.notificationService.error('Failed to send email report');
      }
    });
  }
}