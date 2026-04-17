// core/validation/validation.service.ts
import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class ValidationService {
  
  validateUserId(userId: string): { valid: boolean; message: string } {
    if (!userId) 
      return { valid: false, message: 'User ID is required' };
    
    if (userId.length !== 3)
      return { valid: false, message: 'User ID must be exactly 3 characters' };
    
    if (!/^[A-Za-z0-9]{3}$/.test(userId))
      return { valid: false, message: 'User ID must be alphanumeric only' };
    
    return { valid: true, message: '' };
  }
  
  validatePassword(password: string): { valid: boolean; message: string } {
    if (!password) {
      return { valid: false, message: 'Password is required' };
    }
    if (password.length < 6) {
      return { valid: false, message: 'Password must be at least 6 characters' };
    }
    if (password.length > 20) {
      return { valid: false, message: 'Password cannot exceed 20 characters' };
    }
    return { valid: true, message: '' };
  }
  
  validatePasswordMatch(password: string, confirmPassword: string): { valid: boolean; message: string } {
    if (password !== confirmPassword) {
      return { valid: false, message: 'Passwords do not match' };
    }
    return { valid: true, message: '' };
  }
  
  validateName(name: string): { valid: boolean; message: string } {
    if (!name) {
      return { valid: false, message: 'Name is required' };
    }
    if (name.length < 2) {
      return { valid: false, message: 'Name must be at least 2 characters' };
    }
    if (name.length > 100) {
      return { valid: false, message: 'Name cannot exceed 100 characters' };
    }
    return { valid: true, message: '' };
  }
  
  validateEmail(email: string): { valid: boolean; message: string } {
    if (!email) {
      return { valid: false, message: 'Email is required' };
    }
    const emailRegex = /^[^\s@]+@([^\s@]+\.)+[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return { valid: false, message: 'Please enter a valid email address' };
    }
    return { valid: true, message: '' };
  }
  
  validateRegisterForm(user: any): { valid: boolean; errors: string[] } {
    const errors: string[] = [];
    
    const userIdValidation = this.validateUserId(user.userId);
    if (!userIdValidation.valid) errors.push(userIdValidation.message);
    
    const passwordValidation = this.validatePassword(user.password);
    if (!passwordValidation.valid) errors.push(passwordValidation.message);
    
    const passwordMatchValidation = this.validatePasswordMatch(user.password, user.confirmPassword);
    if (!passwordMatchValidation.valid) errors.push(passwordMatchValidation.message);
    
    const nameValidation = this.validateName(user.name);
    if (!nameValidation.valid) errors.push(nameValidation.message);
    
    const emailValidation = this.validateEmail(user.email);
    if (!emailValidation.valid) errors.push(emailValidation.message);
    
    return {
      valid: errors.length === 0,
      errors: errors
    };
  }
}