import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { PaymentHomeComponent } from './payment-home.component';

const routes: Routes = [
  { path: ':orderNumber', component: PaymentHomeComponent },
  { path: '', redirectTo: '/order', pathMatch: 'full' }
];

@NgModule({
  declarations: [PaymentHomeComponent],
  imports: [CommonModule, RouterModule.forChild(routes)]
})
export class PaymentModule {}
