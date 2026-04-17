import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/auth/auth';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  credentials = {
    userId: '',
    password: ''
  };
  
  isLoading = false;
  errorMessage = '';
  
  constructor(
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  onSubmit(): void {
    this.errorMessage = '';
    
    if (!this.credentials.userId || !this.credentials.userId.trim()) {
      this.errorMessage = 'Please enter your User ID';
      return;
    }
    
    if (!this.credentials.password) {
      this.errorMessage = 'Please enter your password';
      return;
    }
    
    this.isLoading = true;
    
    this.authService.login(this.credentials).subscribe({
      next: (response) => {
        this.authService.saveAuthData(response);
        this.isLoading = false;
        
        const redirectUrl = localStorage.getItem('redirectUrl');
        const lastVisitedUrl = localStorage.getItem('lastVisitedUrl');
        
        if (redirectUrl && redirectUrl !== '/login' && redirectUrl !== '/register') {
          localStorage.removeItem('redirectUrl');
          this.router.navigateByUrl(redirectUrl);
        } else if (lastVisitedUrl && lastVisitedUrl !== '/login' && lastVisitedUrl !== '/register')
          this.router.navigateByUrl(lastVisitedUrl);
        else this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.isLoading = false;
        
        if (err.error?.message === 'User not found')
          this.errorMessage = 'User not found. Please check your User ID.';
        else if (err.status === 401)
          this.errorMessage = 'Invalid credentials';
        else if (err.status === 404)
          this.errorMessage = 'User not found';
        else if (err.status > 499)
          this.errorMessage = 'The API might be down, please retry.';
        else
          this.errorMessage = 'Login failed. Please try again.';
        
        
        console.error('Login error:', err);
      }
    });
  }
}