// shared/components/crud-modal/crud-modal.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormGroup } from '@angular/forms';

@Component({
  selector: 'app-crud-modal',
  templateUrl: './crud-modal.component.html',
  styleUrls: ['./crud-modal.component.css']
})
export class CrudModalComponent {
  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() formGroup!: FormGroup;
  @Input() fields: Array<{
    key: string;
    label: string;
    type: 'text' | 'number' | 'select' | 'textarea' | 'email' | 'url';
    required?: boolean;
    options?: Array<{ value: any; label: string }>;
  }> = [];
  
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<any>();
  
  onSubmit(): void {
    if (this.formGroup.valid) {
      this.submit.emit(this.formGroup.value);
      this.close.emit();
    } else {
      // Mark all fields as touched to show validation errors
      Object.keys(this.formGroup.controls).forEach(key => {
        this.formGroup.get(key)?.markAsTouched();
      });
    }
  }
  
  onClose(): void {
    this.close.emit();
  }
}