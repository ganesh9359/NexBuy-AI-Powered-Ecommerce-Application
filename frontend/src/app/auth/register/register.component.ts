import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService, AuthResponse } from '../core/auth.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  loading = false;
  error = '';
  otpStep = false;
  pendingUserId: number | null = null;
  emailForOtp = '';

  form = this.fb.group({
    firstName: ['', Validators.required],
    lastName: [''],
    phone: [''],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    line1: ['', Validators.required],
    city: ['', Validators.required],
    state: ['', Validators.required],
    postalCode: ['', Validators.required],
    country: ['', Validators.required],
    otp: ['']
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    if (this.otpStep) {
      this.submitOtp();
      return;
    }
    this.normalizeEmail();
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    const v = this.form.value;
    const payload: any = {
      firstName: v.firstName,
      lastName: v.lastName,
      phone: v.phone,
      email: v.email,
      password: v.password,
      address: {
        line1: v.line1,
        city: v.city,
        state: v.state,
        postalCode: v.postalCode,
        country: v.country
      }
    };

    this.auth.register(payload).subscribe({
      next: (res) => this.handleAuthResponse(res, 'register'),
      error: (err) => this.handleError(err)
    });
  }

  private submitOtp(): void {
    if (!this.pendingUserId || !this.form.value.otp) {
      this.error = 'Enter the OTP sent to your email/phone';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.verifyOtp({ userId: this.pendingUserId, otp: this.form.value.otp, purpose: 'register' }).subscribe({
      next: (res) => this.handleAuthResponse(res, 'register'),
      error: (err) => this.handleError(err)
    });
  }

  private handleAuthResponse(res: AuthResponse, purpose: 'login' | 'register') {
    this.loading = false;
    if (res.requiresOtp) {
      this.otpStep = true;
      this.pendingUserId = res.userId || null;
      this.emailForOtp = res.email;
      return;
    }
    this.otpStep = false;
    this.pendingUserId = null;
    this.router.navigate(['/']);
  }

  resend(): void {
    if (!this.pendingUserId) return;
    this.loading = true;
    this.error = '';
    this.auth.resendOtp({ userId: this.pendingUserId, purpose: 'register' }).subscribe({
      next: () => { this.loading = false; },
      error: (err) => this.handleError(err)
    });
  }

  private handleError(err: any) {
    this.loading = false;
    this.error = err?.error?.message || 'Something went wrong';
  }

  socialLogin(provider: string): void {
    window.location.href = `${environment.apiBase}/oauth2/authorize/${provider}`;
  }

  private normalizeEmail(): void {
    const control = this.form.get('email');
    const normalized = ((control?.value as string | null) || '').replace(/\s+/g, '').trim().toLowerCase();
    control?.setValue(normalized, { emitEvent: false });
    control?.updateValueAndValidity({ emitEvent: false });
  }
}