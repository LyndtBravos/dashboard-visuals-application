import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LoadingService } from '../../../core/services/loading';

@Component({
  selector: 'app-global-loader',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-loader.html',
  styleUrls: ['./global-loader.css']
})
export class GlobalLoaderComponent implements OnInit {
  isLoading = false;
  
  constructor(public loadingService: LoadingService,
    public cdr: ChangeDetectorRef
  ) {}
  
  ngOnInit(): void {
    this.loadingService.loading$.subscribe(loading => {
      this.isLoading = loading;
      this.cdr.markForCheck();
    });
  }
}