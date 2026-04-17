import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Subject, debounceTime, takeUntil } from 'rxjs';

export interface FieldConfig {
  key: string;
  label: string;
  type: 'text' | 'textarea' | 'number' | 'select' | 'checkbox' | 'email' | 'url' | 'time' | 'multiselect';
  required?: boolean;
  min?: number;
  max?: number;
  minLength?: number;
  maxLength?: number;
  pattern?: string;
  defaultValue?: any;
  options?: Array<{ value: any; label: string }>;
  conditionallyRequired?: (formValue: any) => boolean;
  hidden?: (formValue: any) => boolean;
  validators?: any[];
  placeholder?: string;
}

@Component({
  selector: 'app-universal-crud-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './universal-crud-modal.html',
  styleUrls: ['./universal-crud-modal.css']
})
export class UniversalCrudModalComponent implements OnInit, OnDestroy {
  @Input() isOpen: boolean = false;
  @Input() title: string = '';
  @Input() mode: 'create' | 'edit' = 'create';
  @Input() fields: FieldConfig[] = [];
  @Input() initialData: any = null;
  @Input() submitButtonText: string = 'Save';
  @Input() isLoading: boolean = false;
  
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<any>();
  
  formGroup!: FormGroup;
  private destroy$ = new Subject<void>();
  
  constructor(private fb: FormBuilder) {}
  
  ngOnInit(): void {
    this.buildForm();

    this.formGroup.valueChanges
      .pipe(debounceTime(100), takeUntil(this.destroy$))
      .subscribe(value => {
        this.updateConditionalValidators(value);
        this.updateConditionalVisibility(value);
      });
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  buildForm(): void {
    const formControls: any = {};
    
    this.fields.forEach(field => {
      let value = this.initialData?.[field.key] ?? field.defaultValue ?? '';

      const validators = this.buildValidators(field);
      
      formControls[field.key] = [value, validators];
    });
    
    this.formGroup = this.fb.group(formControls);
  }
  
  buildValidators(field: FieldConfig): any[] {
    const validators = [];
    
    if (field.required) 
      validators.push(Validators.required);
      
    if (field.min !== undefined)
      validators.push(Validators.min(field.min));
    
    if (field.max !== undefined)
      validators.push(Validators.max(field.max));
    
    if (field.minLength !== undefined) 
      validators.push(Validators.minLength(field.minLength));
    
    if (field.maxLength !== undefined)
      validators.push(Validators.maxLength(field.maxLength));
    
    if (field.pattern) 
      validators.push(Validators.pattern(field.pattern));
    
    if (field.type === 'email')
      validators.push(Validators.email);
    
    if (field.type === 'url') 
      validators.push(Validators.pattern('^(http|https)://.*$'));
    
    return validators;
  }
  
  updateConditionalValidators(formValue: any): void {
    this.fields.forEach(field => {
      const control = this.formGroup.get(field.key);
      if (!control) return;
      
      // Check if field is conditionally required
      if (field.conditionallyRequired && field.conditionallyRequired(formValue)) {
        if (!control.hasValidator(Validators.required)) {
          control.addValidators(Validators.required);
          control.updateValueAndValidity();
        }
      } else {
        if (control.hasValidator(Validators.required)) {
          control.removeValidators(Validators.required);
          control.updateValueAndValidity();
        }
      }
    });
  }
  
  updateConditionalVisibility(formValue: any): void {
    // For visibility changes, we just let the template handle it
    // The form still contains the value but it's hidden
  }
  
  isFieldVisible(field: FieldConfig): boolean {
    if (field.hidden) {
      return !field.hidden(this.formGroup.value);
    }
    return true;
  }
  
  getFieldError(fieldKey: string): string {
    const control = this.formGroup.get(fieldKey);
    if (!control || !control.errors || !control.touched) return '';
    
    const errors = control.errors;
    
    if (errors['required']) return 'This field is required';
    if (errors['email']) return 'Please enter a valid email address';
    if (errors['min']) return `Minimum value is ${errors['min'].min}`;
    if (errors['max']) return `Maximum value is ${errors['max'].max}`;
    if (errors['minlength']) return `Minimum length is ${errors['minlength'].requiredLength} characters`;
    if (errors['maxlength']) return `Maximum length is ${errors['maxlength'].requiredLength} characters`;
    if (errors['pattern']) return 'Invalid format';
    
    return 'Invalid input';
  }
  
  onSubmit(): void {
    if (this.formGroup.valid) 
      this.submit.emit(this.formGroup.value);
    else 
      Object.keys(this.formGroup.controls).forEach(key => {
        this.formGroup.get(key)?.markAsTouched();
      });
  }
  
  onClose(): void {
    this.close.emit();
  }
  
  compareSelectOptions(optionValue: any, selectedValue: any): boolean {
    return optionValue === selectedValue;
  }
  
  toggleMultiSelectOption(fieldKey: string, optionValue: any): void {
    const control = this.formGroup.get(fieldKey);
    if (!control) return;
    
    let currentValue = control.value || [];
    if (Array.isArray(currentValue)) 
      if (currentValue.includes(optionValue))
        control.setValue(currentValue.filter((v: any) => v !== optionValue));
      else
        control.setValue([...currentValue, optionValue]);
    else 
      control.setValue([optionValue]);
  }
  
  isMultiSelectSelected(fieldKey: string, optionValue: any): boolean {
    const control = this.formGroup.get(fieldKey);
    if (!control) return false;
    
    const value = control.value;
    if (Array.isArray(value)) 
      return value.includes(optionValue);
    
    return value === optionValue;
  }
}