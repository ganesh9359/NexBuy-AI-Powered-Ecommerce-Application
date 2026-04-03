import { Component, OnInit } from '@angular/core';
import { AdminApiService, AdminUser } from './admin-api.service';

@Component({
  selector: 'app-admin-admins',
  templateUrl: './admin-admins.component.html',
  styleUrls: ['./admin-admins.component.scss']
})
export class AdminAdminsComponent implements OnInit {
  admins: AdminUser[] = [];
  loading = true;
  saving = false;
  error = '';
  success = '';
  form = {
    email: '',
    password: '',
    phone: ''
  };

  constructor(private adminApi: AdminApiService) {}

  ngOnInit(): void {
    this.loadAdmins();
  }

  createAdmin(): void {
    if (!this.form.email.trim() || !this.form.password.trim()) {
      this.error = 'Email and password are required';
      return;
    }
    this.saving = true;
    this.error = '';
    this.success = '';
    this.adminApi.createAdmin(this.form).subscribe({
      next: (admin) => {
        this.admins = [admin, ...this.admins];
        this.form = { email: '', password: '', phone: '' };
        this.success = 'New admin added successfully.';
        this.saving = false;
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not create admin';
        this.saving = false;
      }
    });
  }

  private loadAdmins(): void {
    this.loading = true;
    this.adminApi.getAdmins().subscribe({
      next: (admins) => {
        this.admins = admins;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}