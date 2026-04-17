import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { LocalStorageService } from './local-storage';

@Injectable({
  providedIn: 'root'
})
export class PaginationStorageService {
  
  constructor(private localStorageService: LocalStorageService) {}
  
  getPageSize(pageKey: string, defaultValue: number = 10): number {
    const key = `pagination_${pageKey}_pageSize`;
    const saved = this.localStorageService.getItem<number>(key);
    return saved !== null ? saved : defaultValue;
  }
  
  setPageSize(pageKey: string, pageSize: number): void {
    const key = `pagination_${pageKey}_pageSize`;
    this.localStorageService.setItem(key, pageSize);
  }
  
  getSort(pageKey: string, defaultValue: { key: string; direction: 'asc' | 'desc' } = { key: 'id', direction: 'asc' }): { key: string; direction: 'asc' | 'desc' } {
    const key = `pagination_${pageKey}_sort`;
    const saved = this.localStorageService.getItem<{ key: string; direction: 'asc' | 'desc' }>(key);
    return saved !== null ? saved : defaultValue;
  }
  
  setSort(pageKey: string, sortKey: string, direction: 'asc' | 'desc'): void {
    const key = `pagination_${pageKey}_sort`;
    this.localStorageService.setItem(key, { key: sortKey, direction });
  }
  
  getSearchTerm(pageKey: string, defaultValue: string = ''): string {
    const key = `pagination_${pageKey}_search`;
    const saved = this.localStorageService.getItem<string>(key);
    return saved !== null ? saved : defaultValue;
  }
  
  setSearchTerm(pageKey: string, searchTerm: string): void {
    const key = `pagination_${pageKey}_search`;
    this.localStorageService.setItem(key, searchTerm);
  }
  
  clearPagePreferences(pageKey: string): void {
    this.localStorageService.removeItem(`pagination_${pageKey}_pageSize`);
    this.localStorageService.removeItem(`pagination_${pageKey}_sort`);
    this.localStorageService.removeItem(`pagination_${pageKey}_search`);
  }
}