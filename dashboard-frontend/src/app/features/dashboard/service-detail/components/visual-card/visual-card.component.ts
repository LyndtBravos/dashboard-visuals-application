import { Component, Input, Output, EventEmitter, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { QueryService, QueryResult, QueryDataSet } from '../../../../../core/services/query';

Chart.register(...registerables);

@Component({
  selector: 'app-visual-card',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './visual-card.component.html',
  styleUrls: ['./visual-card.component.css']
})
export class VisualCardComponent implements OnInit, OnDestroy {
  @Input() visual: any;
  @Output() edit = new EventEmitter<any>();
  @Output() delete = new EventEmitter<any>();
  @Output() recheck = new EventEmitter<any>();
  
  chart: any;
  isLoading = true;
  currentValue: any = null;
  currentStatus: 'green' | 'yellow' | 'red' | 'error' = 'green';
  lastChecked: Date | null = null;
  chartData: any = null;
  errorMessage: string | null = null;
  
  constructor(
    private queryService: QueryService,
    private cdr: ChangeDetectorRef
  ) {}
  
  ngOnInit(): void {
    this.loadData();
  }
  
  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
  }
  
  refreshData(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading = true;
    this.errorMessage = null;
    
    const graphType = this.visual.graphType;
    const query = this.visual.queryText;
    const warning = this.visual.thresholdWarning;
    const danger = this.visual.thresholdDanger;
    
    if (!query || query.trim() === '') {
      this.isLoading = false;
      this.errorMessage = 'No query defined';
      this.cdr.detectChanges();
      return;
    }

    if (graphType === 'bar' || graphType === 'time_series') {
      this.queryService.executeQueryDataset(query).subscribe({
        next: (result: QueryDataSet) => {
          this.isLoading = false;
          
          if (result.success && result.data && result.data.length > 0) {
            this.processChartData(result);
            this.currentStatus = this.getStatusFromChartData(result);
            this.lastChecked = new Date();
            setTimeout(() => this.initChart(), 100);
          } else {
            this.errorMessage = result.errorMessage || 'No data returned';
          }
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error(`[${this.visual.name}] Chart error:`, error);
          this.isLoading = false;
          this.errorMessage = error.message || 'Failed to load data';
          this.cdr.detectChanges();
        }
      });
    } else {
      this.queryService.executeQuery(query, warning, danger).subscribe({
        next: (result: QueryResult) => {
          this.isLoading = false;
          this.currentValue = result.value;
          this.currentStatus = result.status;
          this.lastChecked = new Date();
          this.cdr.detectChanges();
        },
        error: (error) => {
          console.error(`[${this.visual.name}] Text error:`, error);
          this.isLoading = false;
          this.errorMessage = error.message || 'Failed to load data';
          this.cdr.detectChanges();
        }
      });
    }
  }
  
  getStatusFromChartData(result: QueryDataSet): 'green' | 'yellow' | 'red' {
    if (!result.data || result.data.length === 0) return 'green';
    
    const warning = this.visual.thresholdWarning;
    const danger = this.visual.thresholdDanger;
    
    // Get all numeric values from the data
    let allValues: number[] = [];
    for (const row of result.data) {
      for (const key in row) {
        const val = row[key];
        if (typeof val === 'number') {
          allValues.push(val);
        } else if (typeof val === 'string' && !isNaN(parseFloat(val))) {
          allValues.push(parseFloat(val));
        }
      }
    }
    
    const maxValue = Math.max(...allValues);
    if (danger && maxValue >= danger) return 'red';
    if (warning && maxValue >= warning) return 'yellow';
    return 'green';
  }
  
  processChartData(result: QueryDataSet): void {
    if (!result.data || result.data.length === 0) return;
    
    const columns = result.columns;
    const xAxisCol = this.visual.xAxisColumn || columns[0];
    
    let yAxisCols: string[] = [];
    if (this.visual.yAxisColumns) {
      try {
        yAxisCols = typeof this.visual.yAxisColumns === 'string'
          ? JSON.parse(this.visual.yAxisColumns)
          : this.visual.yAxisColumns;
      } catch {
        yAxisCols = columns.filter(col => col !== xAxisCol);
      }
    } else {
      yAxisCols = columns.filter(col => col !== xAxisCol);
    }
    
    const labels = result.data.map(row => {
      let value = row[xAxisCol];
      
      if (typeof value === 'string') {
        // Check if it's an ISO date format (YYYY-MM-DD or YYYY-MM-DDTHH:mm:ss)
        const isISODate = value.match(/^\d{4}-\d{2}-\d{2}(T\d{2}:\d{2}:\d{2})?/);
        
        if (isISODate) {
          const parsedDate = new Date(value);
          if (!isNaN(parsedDate.getTime())) {
            // For date-only strings (YYYY-MM-DD), show short weekday
            if (value.match(/^\d{4}-\d{2}-\d{2}$/))
              return parsedDate.toLocaleDateString('en-US', { weekday: 'short' });
            
            if (value.includes('T'))
              return this.convertToLocalTime(value);              
          }
        }
      }
      return value;
    });
    
    const valuesBySeries: { [key: string]: number[] } = {};
    yAxisCols.forEach(col => {
      valuesBySeries[col] = result.data.map(row => {
        const val = row[col];
        if (val === null || val === undefined) return 0;
        if (typeof val === 'number') return val;
        const num = parseFloat(val);
        return isNaN(num) ? 0 : num;
      });
    });
    
    this.chartData = {
      labels: labels,
      values: valuesBySeries,
      rowCount: result.data.length
    };
  }

  initChart(): void {
    if (!this.chartData || !this.chartData.labels || this.chartData.labels.length === 0) return;
    
    const canvasId = `chart-${this.visual.id}`;
    const canvas = document.getElementById(canvasId) as HTMLCanvasElement;
    if (!canvas) return;
    
    if (this.chart) this.chart.destroy();
    
    const chartType = this.visual.graphType === 'bar' ? 'bar' : 'line';
    
    // Define distinct colors for each series
    const colorPalette = [
      { bg: 'rgba(59, 130, 246, 0.7)', border: 'rgb(59, 130, 246)' },   // Blue
      { bg: 'rgba(16, 185, 129, 0.7)', border: 'rgb(16, 185, 129)' },   // Green
      { bg: 'rgba(239, 68, 68, 0.7)', border: 'rgb(239, 68, 68)' },     // Red
      { bg: 'rgba(245, 158, 11, 0.7)', border: 'rgb(245, 158, 11)' },   // Orange
      { bg: 'rgba(139, 92, 246, 0.7)', border: 'rgb(139, 92, 246)' },   // Purple
      { bg: 'rgba(236, 72, 153, 0.7)', border: 'rgb(236, 72, 153)' }     // Pink
    ];
    
    const datasets = Object.keys(this.chartData.values).map((key, index) => {
      const colors = colorPalette[index % colorPalette.length];
      return {
        label: key,
        data: this.chartData.values[key],
        backgroundColor: colors.bg,
        borderColor: colors.border,
        borderWidth: 2,
        fill: false,
        tension: 0.4
      };
    });
    
    try {
      this.chart = new Chart(canvas, {
        type: chartType,
        data: { labels: this.chartData.labels, datasets: datasets },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'top' },
            tooltip: { mode: 'index', intersect: false }
          },
          scales: {
            y: { beginAtZero: true, grid: { color: '#374151' }, ticks: { color: '#9ca3af' } },
            x: { grid: { display: false }, ticks: { color: '#9ca3af', maxRotation: 45, minRotation: 45 } }
          }
        }
      });
    } catch (error) {
      console.error('Chart error:', error);
      this.errorMessage = 'Failed to render chart';
    }
  }

  convertToLocalTime(utcDateString: string): string {
    return utcDateString.split('T')[1].trim();
  }

  getCardWidth(): string {
    const textTypes = ['text', 'text_minutes', 'status_indicator'];
    if (textTypes.includes(this.visual.graphType))
      return '320px';
    
    const widths: { [key: string]: string } = {
      small: '320px', medium: '520px', large: '820px', xl: '1020px', full: '100%'
    };
    return widths[this.visual.width] || '400px';
  }

  getChartHeight(): string {
    const heights: { [key: string]: string } = {
      small: '200px', medium: '250px', large: '300px', xl: '350px', full: '400px'
    };
    return heights[this.visual.width] || '250px';
  }
  
  getStatusColor(): string {
    const colors: { [key: string]: string } = {
      green: 'status-green', yellow: 'status-yellow', red: 'status-red'
    };
    return colors[this.currentStatus] || '';
  }
  
  formatValue(): string {
    if (this.visual.graphType === 'text_minutes') return `${this.currentValue} min`;
    return this.currentValue;
  }
  
  onRecheck(): void {
    this.recheck.emit(this.visual);
    this.loadData();
  }
  
  onEdit(): void {
    this.edit.emit(this.visual);
  }
  
  onDelete(): void {
    this.delete.emit(this.visual);
  }
}