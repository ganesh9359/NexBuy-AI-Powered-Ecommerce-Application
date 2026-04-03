import { Component, OnInit } from '@angular/core';
import { AdminApiService, AdminUser } from './admin-api.service';

@Component({
  selector: 'app-admin-users',
  templateUrl: './admin-users.component.html',
  styleUrls: ['./admin-users.component.scss']
})
export class AdminUsersComponent implements OnInit {
  users: AdminUser[] = [];
  query = '';
  loading = true;

  constructor(private adminApi: AdminApiService) {}

  ngOnInit(): void {
    this.adminApi.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  get filteredUsers(): AdminUser[] {
    const term = this.query.trim().toLowerCase();
    if (!term) {
      return this.users;
    }
    return this.users.filter((user) =>
      [user.email, user.phone || '', user.role, user.status].some((value) => value.toLowerCase().includes(term))
    );
  }
}