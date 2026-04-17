import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser, isPlatformServer } from '@angular/common';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {
  private isBrowser: boolean;
  
  constructor(@Inject(PLATFORM_ID) private platformId: any) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }
  
  setItem(key: string, value: any): boolean {
    if (!this.isBrowser) {
      console.warn(`localStorage.setItem called on server for key: ${key}`);
      return false;
    }
    
    try {
      const stringValue = typeof value === 'string' ? value : JSON.stringify(value);
      localStorage.setItem(key, stringValue);
      return true;
    } catch (error) {
      console.error(`Error setting localStorage key "${key}":`, error);
      return false;
    }
  }
  
  getItem<T = any>(key: string, defaultValue?: T): T | null {
    if (!this.isBrowser) 
      return defaultValue ?? null;
        
    try {
      const item = localStorage.getItem(key);
      if (item === null)
        return defaultValue ?? null;
      
      try {
        return JSON.parse(item) as T;
      } catch {
        return item as unknown as T;
      }
    } catch (error) {
      console.error(`Error getting localStorage key "${key}":`, error);
      return defaultValue ?? null;
    }
  }
  
  /**
   * Remove item from localStorage
   */
  removeItem(key: string): boolean {
    if (!this.isBrowser) {
      return false;
    }
    
    try {
      localStorage.removeItem(key);
      return true;
    } catch (error) {
      console.error(`Error removing localStorage key "${key}":`, error);
      return false;
    }
  }
  
  /**
   * Clear all localStorage
   */
  clear(): boolean {
    if (!this.isBrowser) {
      return false;
    }
    
    try {
      localStorage.clear();
      return true;
    } catch (error) {
      console.error('Error clearing localStorage:', error);
      return false;
    }
  }
  
  /**
   * Check if key exists in localStorage
   */
  has(key: string): boolean {
    if (!this.isBrowser) {
      return false;
    }
    
    return localStorage.getItem(key) !== null;
  }
}