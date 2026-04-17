import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertDetail } from '../../../core/services/alert';

@Component({
  selector: 'app-alert-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alert-detail-modal.html',
  styleUrls: ['./alert-detail-modal.css']
})
export class AlertDetailModalComponent {
  @Input() alert: AlertDetail | null = null;
  @Output() close = new EventEmitter<void>();
  
  getSeverityClass(): string {
    switch (this.alert?.severity) {
      case 'critical': return 'severity-critical';
      case 'high': return 'severity-high';
      case 'medium': return 'severity-medium';
      case 'low': return 'severity-low';
      default: return '';
    }
  }
  
  getMonitorTypeIcon(): string {
    switch (this.alert?.monitorType) {
      case 'site': return '🌐';
      case 'api': return '🔌';
      case 'server': return '🖥️';
      case 'query': return '📊';
      default: return '⚠️';
    }
  }
  
  getMonitorTypeLabel(): string {
    switch (this.alert?.monitorType) {
      case 'site': return 'Site Monitor';
      case 'api': return 'API Monitor';
      case 'server': return 'Server Monitor';
      case 'query': return 'Dashboard Visual';
      default: return 'Unknown';
    }
  }
  
  formatDate(date: string | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleString();
  }
}