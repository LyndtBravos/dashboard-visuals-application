import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrls: ['./register.css']
})
export class RegisterComponent {
  user = {
    userId: '',
    password: '',
    confirmPassword: '',
    name: '',
    email: ''
  };
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  
  constructor(
    private authService: AuthService,
    private router: Router
  ) {}
  
  validateForm(): boolean {
    if (!this.user.userId || this.user.userId.length !== 3) {
      this.errorMessage = 'User ID must be exactly 3 characters';
      return false;
    }
    
    if (!this.user.password || this.user.password.length < 6) {
      this.errorMessage = 'Password must be at least 6 characters';
      return false;
    }
    
    if (this.user.password !== this.user.confirmPassword) {
      this.errorMessage = 'Passwords do not match';
      return false;
    }
    
    if (!this.user.name) {
      this.errorMessage = 'Name is required';
      return false;
    }
    
    if (!this.user.email || !this.user.email.includes('@')) {
      this.errorMessage = 'Valid email is required';
      return false;
    }
    
    return true;
  }
  
  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';
    
    if (!this.validateForm()) {
      return;
    }
    
    this.isLoading = true;
    
    this.authService.register(this.user).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Registration successful! Redirecting to login...';
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (error: any) => {
        this.isLoading = false;
        console.error('Registration error:', error);
        
        if (error.status === 409) {
          this.errorMessage = 'User ID already exists';
        } else if (error.error?.message) {
          this.errorMessage = error.error.message;
        } else {
          this.errorMessage = 'Registration failed. Please try again.';
        }
      }
    });
  }
}