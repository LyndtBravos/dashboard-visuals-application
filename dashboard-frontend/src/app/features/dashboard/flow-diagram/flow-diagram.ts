import { Component, Input, Output, EventEmitter, OnInit, AfterViewInit, OnDestroy, ElementRef, ViewChildren, QueryList, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VisualCardComponent } from '../service-detail/components/visual-card/visual-card.component';
import { DashboardConfig } from '../../../core/services/dashboard-overview';

interface CardPosition {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  row: number;
  col: number;
}

@Component({
  selector: 'app-flow-diagram',
  standalone: true,
  imports: [CommonModule, VisualCardComponent],
  templateUrl: './flow-diagram.html',
  styleUrls: ['./flow-diagram.css']
})
export class FlowDiagramComponent implements OnInit, AfterViewInit, OnDestroy {
  @Input() visuals: DashboardConfig[] = [];
  @Input() serviceType: string = '';
  @Input() searchActive: boolean = false;
  
  @Output() editVisual = new EventEmitter<DashboardConfig>();
  @Output() deleteVisual = new EventEmitter<number>();
  @Output() recheckVisual = new EventEmitter<DashboardConfig>();
  
  @ViewChildren('visualCard') visualCards!: QueryList<ElementRef>;
  
  rows: DashboardConfig[][] = [];
  arrows: Array<{ path: string; color: string; dashArray: string }> = [];
  private resizeObserver: ResizeObserver | null = null;
  
  constructor(private cdr: ChangeDetectorRef) {}
  
  ngOnInit(): void {
    this.arrangeRows();
  }
  
  ngAfterViewInit(): void {
    setTimeout(() => {
      this.drawArrows();
      this.cdr.detectChanges();
    }, 500);
    
    this.resizeObserver = new ResizeObserver(() => {
      setTimeout(() => {
        this.drawArrows();
        this.cdr.detectChanges();
      }, 100);
    });
    
    const container = document.querySelector('.flow-container');
    if (container)
      this.resizeObserver.observe(container);
  }
  
  ngOnDestroy(): void {
    if (this.resizeObserver)
      this.resizeObserver.disconnect();
  }
  
  arrangeRows(): void {
    // Separate visuals with and without flow_order
    const textVisualsWithOrder = this.visuals
      .filter(v => v.flowOrder !== null && v.flowOrder !== undefined && v.flowOrder > 0 &&
                  (v.graphType === 'text' || v.graphType === 'text_minutes' || v.graphType === 'status_indicator'))
      .sort((a, b) => (a.flowOrder || 0) - (b.flowOrder || 0));
    
    const textVisualsWithoutOrder = this.visuals
      .filter(v => (v.flowOrder === null || v.flowOrder === undefined || v.flowOrder === 0) && 
                  (v.graphType === 'text' || v.graphType === 'text_minutes' || v.graphType === 'status_indicator'));
    
    // Combine: ordered first, then unordered at the end
    const allTextVisuals = [...textVisualsWithOrder, ...textVisualsWithoutOrder];
    
    // Calculate cards per row
    const container = document.querySelector('.flow-container');
    let cardsPerRow = 3;
    if (container) {
      const width = container.clientWidth;
      cardsPerRow = Math.max(1, Math.min(4, Math.floor(width / 340)));
    }
    
    // Arrange in snake pattern
    this.rows = [];
    let direction: 'ltr' | 'rtl' = 'ltr';
    
    for (let i = 0; i < allTextVisuals.length; i += cardsPerRow) {
      let rowVisuals = [...allTextVisuals.slice(i, i + cardsPerRow)];
      if (direction === 'rtl') {
        rowVisuals.reverse();
      }
      this.rows.push(rowVisuals);
      direction = direction === 'ltr' ? 'rtl' : 'ltr';
    }
  }
  
  drawArrows(): void {
    this.arrows = [];
    if (this.searchActive || this.visualCards.length === 0) return;
    
    const colors: { [key: string]: string } = {
      Broadcast: '#3b82f6',
      Print: '#8b5cf6',
      Online: '#10b981'
    };
    const arrowColor = colors[this.serviceType] || '#3b82f6';
    const dashArray = this.serviceType === 'Print' ? '8,4' : (this.serviceType === 'Online' ? '4,4' : 'none');
    
    const cards = this.visualCards.toArray();
    const containerRect = document.querySelector('.flow-container')?.getBoundingClientRect();
    if (!containerRect) return;
    
    const positions: CardPosition[] = [];
    let cardIndex = 0;
    
    for (let r = 0; r < this.rows.length; r++) {
      for (let c = 0; c < this.rows[r].length; c++) {
        if (cardIndex < cards.length) {
          const rect = cards[cardIndex].nativeElement.getBoundingClientRect();
          positions.push({
            id: cardIndex,
            x: rect.left - containerRect.left,
            y: rect.top - containerRect.top,
            width: rect.width,
            height: rect.height,
            row: r,
            col: c
          });
        }
        cardIndex++;
      }
    }
    
    const gap = 6;
    
    for (let i = 0; i < positions.length - 1; i++) {
      const from = positions[i];
      const to = positions[i + 1];
      
      let path = '';
      
      if (from.row === to.row) {
          const fromX = from.x + from.width - gap;
          const toX = to.x + gap;
          const y = from.y + from.height / 2;
          path = `M ${fromX} ${y} L ${toX} ${y}`;
      } else if (from.col === to.col && from.row + 1 === to.row) {
        // Directly below - straight vertical arrow from bottom to top edge
        const fromX = from.x + from.width / 2;
        const toX = to.x + to.width / 2;
        path = `M ${fromX} ${from.y + from.height - gap} L ${toX} ${to.y + gap}`;
        
      } else {
        // Diagonal arrow between rows (straight line, no curve)
        const fromX = from.col < to.col ? from.x + from.width - gap : from.x + gap;
        const fromY = from.y + from.height - gap;
        const toX = from.col < to.col ? to.x + gap : to.x + to.width - gap;
        const toY = to.y + gap;
        path = `M ${fromX} ${fromY} L ${toX} ${toY}`;
      }
      
      this.arrows.push({ path, color: arrowColor, dashArray });
    }
  }
  
  onEdit(visual: DashboardConfig): void {
    this.editVisual.emit(visual);
  }
  
  onDelete(id: number): void {
    this.deleteVisual.emit(id);
  }
  
  onRecheck(visual: DashboardConfig): void {
    this.recheckVisual.emit(visual);
  }
}