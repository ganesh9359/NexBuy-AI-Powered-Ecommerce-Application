import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-forgot',
  templateUrl: './forgot.component.html',
  styleUrls: ['./forgot.component.scss']
})
export class ForgotComponent {
  loading = false;
  success = '';
  error = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService) {}

  submit(): void {
    this.normalizeEmail();
    if (this.form.invalid) return;
    this.loading = true;
    this.success = '';
    this.error = '';
    this.auth.forgot({ email: this.form.value.email as string }).subscribe({
      next: () => {
        this.loading = false;
        this.success = 'OTP sent to your email';
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Request failed';
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