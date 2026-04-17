import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Chart } from 'chart.js';
import { DashboardService, DashboardConfig } from '../../../core/services/dashboard-overview';
import { QueryService, QueryResult, QueryDataSet } from '../../../core/services/query';
import { NotificationService } from '../../../core/services/notification';

@Component({
  selector: 'app-visual-builder',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './visual-builder.html',
  styleUrls: ['./visual-builder.css']
})
export class VisualBuilderComponent implements OnInit, OnDestroy {
  @Input() serviceType: string | null = null;
  @Input() visual: DashboardConfig | null = null;
  @Output() close = new EventEmitter<boolean>();

  visualForm: FormGroup;
  isLoading = false;
  testResult: any = null;
  testStatus: 'green' | 'yellow' | 'red' | 'error' | null = null;
  testErrorMessage: string | null = null;
  testDataSet: QueryDataSet | null = null;
  previewChart: any = null;
  isTesting = false;
  
  availableColumns: string[] = [];

  graphTypes = [
    { value: 'text', label: 'Text', icon: '📝', description: 'Simple text value', showConfig: false },
    { value: 'text_minutes', label: 'Text (Minutes)', icon: '⏱️', description: 'Text formatted as minutes', showConfig: false },
    { value: 'status_indicator', label: 'Status Indicator', icon: '🔴', description: 'Color-coded status', showConfig: false },
    { value: 'bar', label: 'Bar Chart', icon: '📊', description: 'Vertical bar chart', showConfig: true },
    { value: 'time_series', label: 'Time Series', icon: '📈', description: 'Line chart over time', showConfig: true }
  ];

  widths = [
    { value: 'small', label: 'Small (300px)', width: '300px', chartWidth: '100%' },
    { value: 'medium', label: 'Medium (500px)', width: '500px', chartWidth: '100%' },
    { value: 'large', label: 'Large (800px)', width: '800px', chartWidth: '100%' },
    { value: 'xl', label: 'Extra Large (1000px)', width: '1000px', chartWidth: '100%' },
    { value: 'full', label: 'Full Width (100%)', width: '100%', chartWidth: '100%' },
    { value: 'dashboard', label: 'Dashboard Width (Auto)', width: 'auto', chartWidth: '100%' }
  ];

  services = [
    { value: 'Alerts', label: 'Alerts', icon: '⚠️' },
    { value: 'Deliveries', label: 'Deliveries', icon: '🚚' },
    { value: 'Broadcast', label: 'Broadcast', icon: '📡' },
    { value: 'Online', label: 'Online', icon: '🌐' },
    { value: 'Print', label: 'Print', icon: '🖨️' }    
  ];

  aggregationTypes = [
    { value: 'none', label: 'None' },
    { value: 'sum', label: 'Sum' },
    { value: 'avg', label: 'Average' },
    { value: 'count', label: 'Count' },
    { value: 'min', label: 'Minimum' },
    { value: 'max', label: 'Maximum' }
  ];

  timeIntervals = [
    { value: 'hour', label: 'Hour' },
    { value: 'day', label: 'Day' },
    { value: 'week', label: 'Week' },
    { value: 'month', label: 'Month' },
    { value: 'year', label: 'Year' }
  ];

  yAxisSeries: Array<{ column: string; color?: string }> = [];
  selectedYAxisColumn: string = '';

  constructor(private fb: FormBuilder, private dashboardService: DashboardService,
    private queryService: QueryService, private notificationService: NotificationService,
    private cdr: ChangeDetectorRef) {
    this.visualForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(200)]],
      description: ['', [Validators.maxLength(1000)]],
      serviceType: [this.serviceType || 'Broadcast', [Validators.required]],
      graphType: ['text', [Validators.required]],
      flowOrder: [null, [Validators.min(1), Validators.max(200)]],
      queryText: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(5000)]],
      thresholdWarning: [null, [Validators.min(0)]],
      thresholdDanger: [null, [Validators.min(0)]],
      alertEmail: ['', [Validators.email]],
      alert: [false],
      isActive: [true],
      width: ['medium'],
      xAxisColumn: [''],
      yAxisColumns: [''],
      seriesColumns: [''],
      aggregationType: ['none'],
      timeInterval: ['']
    }, { validators: this.validateThresholds });
  }

  ngOnInit(): void {
    const serviceTypeControl = this.visualForm.get('serviceType');

    if (this.serviceType && !this.visual) {
      serviceTypeControl?.setValue(this.serviceType);
      serviceTypeControl?.disable({ emitEvent: false });
    }

    if (!serviceTypeControl?.value)
      serviceTypeControl?.setValue('Broadcast');
    
    if (this.visual) {
      this.populateForm();
      serviceTypeControl?.disable({ emitEvent: false });

      if (this.visual.yAxisColumns)
        try {
          const parsed = JSON.parse(this.visual.yAxisColumns);
          this.yAxisSeries = Array.isArray(parsed)
            ? parsed.map((col: string) => ({ column: col }))
            : [];
        } catch {
          this.yAxisSeries = [];
        }
    }

    this.visualForm.get('xAxisColumn')?.valueChanges.subscribe(() => setTimeout(() => this.updatePreviewChart(), 100));
  }

  ngOnDestroy(): void {}

  shouldShowWidth(): boolean {
    const graphType = this.visualForm.get('graphType')?.value;
    return graphType === 'bar' || graphType === 'time_series';
  }
  
  populateForm(): void {
    let yAxisColumnsValue = this.visual?.yAxisColumns || '';
    if (yAxisColumnsValue && typeof yAxisColumnsValue === 'string')
      try {
        const parsed = JSON.parse(yAxisColumnsValue);
        yAxisColumnsValue = parsed;
      } catch {}
    
    
    this.visualForm.patchValue({
      name: this.visual?.name,
      description: this.visual?.description,
      serviceType: this.visual?.serviceType || this.serviceType || 'Broadcast',
      graphType: this.visual?.graphType,
      flowOrder: this.visual?.flowOrder || null,
      queryText: this.visual?.queryText,
      thresholdWarning: this.visual?.thresholdWarning,
      thresholdDanger: this.visual?.thresholdDanger,
      alertEmail: this.visual?.alertEmail,
      alert: this.visual?.alert,
      isActive: this.visual?.isActive !== undefined ? this.visual.isActive : true,
      width: this.visual?.width || 'medium',
      xAxisColumn: this.visual?.xAxisColumn || '',
      yAxisColumns: yAxisColumnsValue,
      seriesColumns: this.visual?.seriesColumns || '',
      aggregationType: this.visual?.aggregationType || 'none',
      timeInterval: this.visual?.timeInterval || ''
    });
    
    if (this.visual?.yAxisColumns) 
      try {
        const parsed = typeof this.visual.yAxisColumns === 'string' 
          ? JSON.parse(this.visual.yAxisColumns) 
          : this.visual.yAxisColumns;
        this.yAxisSeries = Array.isArray(parsed) ? parsed.map((col: string) => ({ column: col })) : [];
      } catch {
        this.yAxisSeries = [];
      }
  }

  onGraphTypeChange(): void {
    const graphType = this.visualForm.get('graphType')?.value;
    const thresholdWarning = this.visualForm.get('thresholdWarning');
    const thresholdDanger = this.visualForm.get('thresholdDanger');
    const alert = this.visualForm.get('alert');
    const alertEmail = this.visualForm.get('alertEmail');
    
    if (graphType === 'text' || graphType === 'text_minutes' || graphType === 'status_indicator') {
      thresholdWarning?.clearValidators();
      thresholdDanger?.clearValidators();
      alert?.clearValidators();
      alertEmail?.clearValidators();
    } else {
      thresholdWarning?.setValidators([Validators.min(0)]);
      thresholdDanger?.setValidators([Validators.min(0)]);
    }
    
    thresholdWarning?.updateValueAndValidity();
    thresholdDanger?.updateValueAndValidity();
    alert?.updateValueAndValidity();
    alertEmail?.updateValueAndValidity();

    this.testResult = null;
    this.testStatus = null;
    this.testDataSet = null;
    this.testErrorMessage = null;
  }

  getWidthLabel(): string {
    const width = this.visualForm.get('width')?.value;
    const found = this.widths.find(w => w.value === width);
    return found ? found.label : width;
  }

  onAlertChange(): void {
    const alert = this.visualForm.get('alert')?.value;
    const alertEmail = this.visualForm.get('alertEmail');
    
    if (alert)
      alertEmail?.setValidators([Validators.required, Validators.email]);
    else
      alertEmail?.clearValidators();
    
    alertEmail?.updateValueAndValidity();
  }
  
  validateThresholds(group: FormGroup): { [key: string]: any } | null {
    const warning = group.get('thresholdWarning')?.value;
    const danger = group.get('thresholdDanger')?.value;
    
    if (warning !== null && danger !== null && warning >= danger) 
      return { thresholdsInvalid: 'Warning threshold must be less than danger threshold' };
  
    return null;
  }

  isServiceTypeDisabled(): boolean {
    return !!(this.visual || this.serviceType);
  }

  shouldShowChartConfig(): boolean {
    const graphType = this.visualForm.get('graphType')?.value;
    return graphType === 'bar' || graphType === 'time_series';
  }

  testQuery(): void {
    const query = this.visualForm.get('queryText')?.value;
    const graphType = this.visualForm.get('graphType')?.value;

    if (!query) {
      this.testErrorMessage = 'Please enter a query to test';
      return;
    }
    
    this.isTesting = true;
    this.testResult = null;
    this.testStatus = null;
    this.testErrorMessage = null;
    this.testDataSet = null;
    this.availableColumns = [];
    
    if (graphType === 'bar' || graphType === 'time_series') {
      this.queryService.executeQueryDataset(query).subscribe({
        next: (result: QueryDataSet) => {
          this.isTesting = false;
          
          if (result.success && result.data && result.data.length > 0) {
            this.testDataSet = result;
            this.availableColumns = result.columns;
            this.notificationService.success(`Query returned ${result.rowCount} rows`);
            
            if (result.columns.length > 0 && !this.visualForm.get('xAxisColumn')?.value) 
              this.visualForm.patchValue({ xAxisColumn: result.columns[0] });
            
            this.cdr.detectChanges();
          } else {
            this.testErrorMessage = result.errorMessage || 'Query returned no data';
            this.notificationService.warning('Query returned no data');
          }
        },
        error: (error: Error) => {
          this.isTesting = false;
          this.testErrorMessage = error.message || 'Query execution failed';
          this.notificationService.error('Query failed: Unknown error');
        }
      });
    } else {
      const warning = this.visualForm.get('thresholdWarning')?.value;
      const danger = this.visualForm.get('thresholdDanger')?.value;
      
      this.queryService.executeQuery(query, warning, danger).subscribe({
        next: (result: QueryResult) => {
          this.isTesting = false;
          this.testResult = result;
          this.testStatus = result.status;
          this.notificationService.success(`Query returned: ${result.value}`);
        },
        error: (error: Error) => {
          this.isTesting = false;
          this.testErrorMessage = error.message || 'Query execution failed';
          this.notificationService.error('Query failed: Unknown error');
        }
      });
    }
  }

  addYAxisSeries(): void {
    if (this.selectedYAxisColumn && !this.yAxisSeries.some(s => s.column === this.selectedYAxisColumn)) {
      this.yAxisSeries.push({ column: this.selectedYAxisColumn });
      this.selectedYAxisColumn = '';
      this.updateYAxisColumnsField();
    }
  }

  removeYAxisSeries(index: number): void {
    this.yAxisSeries.splice(index, 1);
    this.updateYAxisColumnsField();
  }

  updateYAxisColumnsField(): void {
    const yAxisColumns = this.yAxisSeries.map(s => s.column);
    this.visualForm.patchValue({ yAxisColumns: JSON.stringify(yAxisColumns) });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'green': return '#10b981';
      case 'yellow': return '#f59e0b';
      case 'red': return '#ef4444';
      default: return '#6b7280';
    }
  }

  getPreviewBarColor(value: number): string {
    const warning = this.visualForm.get('thresholdWarning')?.value;
    const danger = this.visualForm.get('thresholdDanger')?.value;
    if (danger && value >= danger) return '#ef4444';
    if (warning && value >= warning) return '#f59e0b';
    return '#10b981';
  }

  isTextVisual(): boolean {
    const type = this.visualForm.get('graphType')?.value;
    return type === 'text' || type === 'text_minutes' || type === 'status_indicator';
  }

  isChartVisual(): boolean {
    const type = this.visualForm.get('graphType')?.value;
    return type === 'bar' || type === 'time_series';
  }

  canSubmit(): boolean {
    if (!this.visualForm) return false;

    if(this.isTextVisual()) return this.validateTextVisual();
    else if(this.isChartVisual()) return this.validateChartVisual();  

    return false;
  }

  validateTextVisual(): boolean {
    const name = this.visualForm.get('name')?.value;
    const query = this.visualForm.get('queryText')?.value;

    if (!name || name.length < 3) return false;
    if (!query || query.length < 5) return false;
    
    if (this.visualForm.get('alert')?.value) {
      const email = this.visualForm.get('alertEmail')?.value;
      if (!email) return false;
    }

    return true;
  }

  validateChartVisual(): boolean {
    const name = this.visualForm.get('name')?.value;
    const query = this.visualForm.get('queryText')?.value;

    if (!name || name.length < 3) return false;
    if (!query || query.length < 5) return false;

    if (!this.testDataSet || this.testDataSet.data.length === 0) return false;

    const xAxis = this.visualForm.get('xAxisColumn')?.value;
    if (!xAxis) return false;

    if (this.yAxisSeries.length === 0) return false;

    return true;
  }

  getFormValidationMessage(): string | null {
    if (this.isTextVisual()) {
      if (!this.visualForm.get('name')?.value) return 'Name is required';
      if (!this.visualForm.get('queryText')?.value) return 'Query is required';

      if (this.visualForm.get('alert')?.value && !this.visualForm.get('alertEmail')?.value) {
        return 'Alert email is required';
      }
    }

    if (this.isChartVisual()) {
      if (!this.testDataSet || this.testDataSet.data.length === 0) {
        return 'Please test a valid query with data';
      }

      if (!this.visualForm.get('xAxisColumn')?.value) {
        return 'Please select an X-axis column';
      }

      if (this.yAxisSeries.length === 0) {
        return 'Please add at least one Y-axis column';
      }
    }

    return null;
  }

  onSubmit(): void {
    if (!this.canSubmit()) {
      this.notificationService.warning(this.getFormValidationMessage() || 'Form is invalid');
      return;
    }
    
    this.isLoading = true;
    const formData = this.visualForm.getRawValue();
    
    if (!formData.thresholdWarning) delete formData.thresholdWarning;
    if (!formData.thresholdDanger) delete formData.thresholdDanger;
    if (!formData.alertEmail) delete formData.alertEmail;
    
    if (formData.yAxisColumns)
      if (typeof formData.yAxisColumns === 'string') {}
      else if (Array.isArray(formData.yAxisColumns))
        formData.yAxisColumns = JSON.stringify(formData.yAxisColumns);
      else if (typeof formData.yAxisColumns === 'object')
        formData.yAxisColumns = JSON.stringify(formData.yAxisColumns);
      
    else if (this.yAxisSeries.length > 0) 
      formData.yAxisColumns = JSON.stringify(this.yAxisSeries.map(s => s.column));

    formData.serviceType = this.visualForm.get('serviceType')?.value || this.serviceType || '';
    
    const request = this.visual
      ? this.dashboardService.updateDashboard(this.visual.id!, formData)
      : this.dashboardService.createDashboard(formData);
    
    request.subscribe({
      next: () => {
        this.isLoading = false;
        this.notificationService.success(
          this.visual ? 'Visual updated successfully' : 'Visual created successfully'
        );
        this.close.emit(true);
      },
      error: (error) => {
        this.isLoading = false;
        this.notificationService.error('Failed to save visual: Unknown error');
        console.error('Error saving visual:', error);
      }
    });
  }

  getPreviewChartData(): any {
    const xAxisColumn = this.visualForm.get('xAxisColumn')?.value;
    const yAxisColumns = this.yAxisSeries.map(s => s.column);
    
    if (!this.testDataSet?.data || !xAxisColumn || yAxisColumns.length === 0) 
      return null;
    
    const labels = this.testDataSet.data.map(row => row[xAxisColumn]);
    const datasets = yAxisColumns.map(col => ({
      label: col,
      data: this.testDataSet?.data.map(row => parseFloat(row[col]) || 0),
      borderColor: this.getRandomColor(),
      backgroundColor: this.getRandomColor(0.2),
      tension: 0.4,
      fill: this.visualForm.get('graphType')?.value === 'time_series'
    }));
    
    return { labels, datasets };
  }

  updatePreviewChart(): void {
    const chartData = this.getPreviewChartData();
    if (!chartData) return;
    
    const canvas = document.getElementById('preview-chart') as HTMLCanvasElement;
    if (!canvas) return;
    
    if (this.previewChart) 
      this.previewChart.destroy();
        
    const chartType = this.visualForm.get('graphType')?.value === 'bar' ? 'bar' : 'line';
    
    this.previewChart = new Chart(canvas, {
      type: chartType,
      data: {
        labels: chartData.labels,
        datasets: chartData.datasets
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: { position: 'top' }
        }
      }
    });
  }

  getRandomColor(opacity: number = 1): string {
    const colors = ['#10b981', '#8b5cf6', '#ef4444', '#3b82f6', '#f59e0b', '#ec489a'];
    return colors[Math.floor(Math.random() * colors.length)];
  }

  onCancel(): void {
    this.close.emit(false);
  }
}