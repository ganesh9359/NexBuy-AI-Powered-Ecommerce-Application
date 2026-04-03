import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { OrderHomeComponent } from './order-home.component';
import { OrderCheckoutComponent } from './order-checkout.component';
import { OrderDetailComponent } from './order-detail.component';
import { OrderStatusComponent } from './order-status.component';

const routes: Routes = [
  { path: '', component: OrderHomeComponent },
  { path: 'checkout', component: OrderCheckoutComponent },
  { path: 'success/:orderNumber', component: OrderStatusComponent, data: { mode: 'success' } },
  { path: 'failure/:orderNumber', component: OrderStatusComponent, data: { mode: 'failure' } },
  { path: ':orderNumber', component: OrderDetailComponent }
];

@NgModule({
  declarations: [OrderHomeComponent, OrderCheckoutComponent, OrderDetailComponent, OrderStatusComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class OrderModule {}
