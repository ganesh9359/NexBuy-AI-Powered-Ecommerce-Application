import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AdminShellComponent } from './admin-shell.component';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { AdminUsersComponent } from './admin-users.component';
import { AdminOrdersComponent } from './admin-orders.component';
import { AdminProductsComponent } from './admin-products.component';
import { AdminProductFormComponent } from './admin-product-form.component';
import { AdminAdminsComponent } from './admin-admins.component';
import { AdminForecastComponent } from './admin-forecast.component';

const routes: Routes = [
  {
    path: '',
    component: AdminShellComponent,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: AdminDashboardComponent, data: { title: 'Dashboard' } },
      { path: 'forecast', component: AdminForecastComponent, data: { title: 'Forecasting' } },
      { path: 'users', component: AdminUsersComponent, data: { title: 'View Users' } },
      { path: 'orders', component: AdminOrdersComponent, data: { title: 'Manage Orders' } },
      { path: 'products', component: AdminProductsComponent, data: { title: 'Manage Products' } },
      { path: 'products/new', component: AdminProductFormComponent, data: { title: 'Add Product' } },
      { path: 'products/:id/edit', component: AdminProductFormComponent, data: { title: 'Edit Product' } },
      { path: 'admins', component: AdminAdminsComponent, data: { title: 'Add Admin' } }
    ]
  }
];

@NgModule({
  declarations: [
    AdminShellComponent,
    AdminDashboardComponent,
    AdminForecastComponent,
    AdminUsersComponent,
    AdminOrdersComponent,
    AdminProductsComponent,
    AdminProductFormComponent,
    AdminAdminsComponent
  ],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class AdminModule {}