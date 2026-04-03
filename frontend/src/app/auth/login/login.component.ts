import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent {
  loading = false;
  error = '';

  form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    this.normalizeEmail();
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    this.auth.login(this.form.value as any).subscribe({
      next: () => {
        this.loading = false;
        const target = this.auth.isAdmin() ? '/admin' : (this.auth.consumeReturnUrl() || '/product');
        this.router.navigateByUrl(target);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.message || 'Login failed';
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