import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaginationStorageService } from '../../../core/storage/pagination-storage';

export interface Column {
  key: string;
  label: string;
  sortable?: boolean;
  type?: 'text' | 'number' | 'date' | 'status';
}

export interface Pagination {
  page: number;
  pageSize: number;
  totalItems: number;
  totalPages: number;
}

@Component({
  selector: 'app-data-table',
  standalone: true,
  imports: [CommonModule, FormsModule ],
  templateUrl: './data-table.component.html',
  styleUrls: ['./data-table.component.css']
})
export class DataTableComponent implements OnInit, OnDestroy {
  @Input() pageKey: string = 'default';
  @Input() columns: Column[] = [];
  @Input() data: any[] = [];
  @Input() totalItems: number = 0;
  @Input() enableCreate: boolean = true;
  @Input() enableEdit: boolean = true;
  @Input() enableDelete: boolean = true;
  @Input() searchPlaceholder: string = 'Search...';
  
  @Output() pageChange = new EventEmitter<number>();
  @Output() pageSizeChange = new EventEmitter<number>();
  @Output() search = new EventEmitter<string>();
  @Output() sort = new EventEmitter<{ key: string, direction: 'asc' | 'desc' }>();
  @Output() create = new EventEmitter<void>();
  @Output() edit = new EventEmitter<any>();
  @Output() delete = new EventEmitter<any>();
  
  currentPage: number = 1;
  currentPageSize: number = 10;
  currentSearchTerm: string = '';
  currentSortKey: string = 'id';
  currentSortDirection: 'asc' | 'desc' = 'asc';
  Math = Math;
  
  pageSizes: number[] = [10, 25, 50, 100, 200];
  totalPages: number = 0;
  
  constructor(private paginationStorage: PaginationStorageService) {}
  
  ngOnInit(): void {
    this.currentPageSize = this.paginationStorage.getPageSize(this.pageKey, 10);
    this.currentSearchTerm = this.paginationStorage.getSearchTerm(this.pageKey, '');
    const savedSort = this.paginationStorage.getSort(this.pageKey);
    this.currentSortKey = savedSort.key;
    this.currentSortDirection = savedSort.direction;
    
    this.pageSizeChange.emit(this.currentPageSize);
    if (this.currentSearchTerm)
      this.search.emit(this.currentSearchTerm);
    
    this.sort.emit({ key: this.currentSortKey, direction: this.currentSortDirection });
  }
  
  ngOnDestroy(): void {
    this.paginationStorage.setPageSize(this.pageKey, this.currentPageSize);
    this.paginationStorage.setSearchTerm(this.pageKey, this.currentSearchTerm);
    this.paginationStorage.setSort(this.pageKey, this.currentSortKey, this.currentSortDirection);
  }
  
  onSearch(): void {
    this.currentPage = 1;
    this.paginationStorage.setSearchTerm(this.pageKey, this.currentSearchTerm);
    this.search.emit(this.currentSearchTerm);
  }
  
  clearSearch(): void {
    this.currentSearchTerm = '';
    this.onSearch();
  }
  
  onSort(key: string): void {
    if (!this.columns.find(c => c.key === key)?.sortable) return;
    
    if (this.currentSortKey === key) 
      this.currentSortDirection = this.currentSortDirection === 'asc' ? 'desc' : 'asc';
    else {
      this.currentSortKey = key;
      this.currentSortDirection = 'asc';
    }
    
    this.paginationStorage.setSort(this.pageKey, this.currentSortKey, this.currentSortDirection);
    this.sort.emit({ key: this.currentSortKey, direction: this.currentSortDirection });
  }
  
  onPageChange(page: number): void {
    this.currentPage = page;
    this.pageChange.emit(page);
  }
  
  onPageSizeChange(size: number): void {
    this.currentPageSize = size;
    this.currentPage = 1;
    this.paginationStorage.setPageSize(this.pageKey, size);
    this.pageSizeChange.emit(size);
    this.pageChange.emit(1);
  }
  
  getSortIcon(key: string): string {
    if (this.currentSortKey !== key) return '↕️';
    return this.currentSortDirection === 'asc' ? '↑' : '↓';
  }
  
  onCreate(): void {
    this.create.emit();
  }
  
  onEdit(item: any): void {
    this.edit.emit(item);
  }
  
  onDelete(item: any): void {
    this.delete.emit(item);
  }
  
  getTotalPages(): number {
    return Math.ceil(this.totalItems / this.currentPageSize);
  }
  
  getPages(): number[] {
    const total = this.getTotalPages();
    const current = this.currentPage;
    const pages: number[] = [];
    
    if (total <= 7) 
      for (let i = 1; i <= total; i++) pages.push(i);
    else
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
        
    return pages;
  }
}