import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-reset',
  templateUrl: './reset.component.html',
  styleUrls: ['./reset.component.scss']
})
export class ResetComponent {
  loading = false;
  success = '';
  error = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    otp: ['', Validators.required],
    newPassword: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    this.normalizeEmail();
    if (this.form.invalid) return;
    this.loading = true;
    this.success = '';
    this.error = '';
    this.auth.reset(this.form.value as any).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'Password reset successful';
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Reset failed';
      }
    });
  }

  private normalizeEmail(): void {
    const control = this.form.get('email');
    const normalized = ((control?.value as string | null) || '').replace(/\s+/g, '').trim().toLowerCase();
    control?.setValue(normalized, { emitEvent: false });
    control?.updateValueAndValidity({ emitEvent: false });
  }
}