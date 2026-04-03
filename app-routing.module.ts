import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { ProductHomeComponent } from './product/product-home.component';
import { AuthGuard } from './auth/core/auth.guard';

const routes: Routes = [
  { path: '', component: ProductHomeComponent, pathMatch: 'full' },
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule) },
  { path: 'user', loadChildren: () => import('./user/user.module').then(m => m.UserModule), canActivate: [AuthGuard] },
  { path: 'product', component: ProductHomeComponent },
  { path: 'cart', loadChildren: () => import('./cart/cart.module').then(m => m.CartModule), canActivate: [AuthGuard] },
  { path: 'order', loadChildren: () => import('./order/order.module').then(m => m.OrderModule), canActivate: [AuthGuard] },
  { path: 'payment', loadChildren: () => import('./payment/payment.module').then(m => m.PaymentModule), canActivate: [AuthGuard] },
  { path: 'admin', loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule), canActivate: [AuthGuard] },
  { path: 'ai', loadChildren: () => import('./ai/ai.module').then(m => m.AiModule) },
  { path: 'integration', loadChildren: () => import('./integration/integration.module').then(m => m.IntegrationModule) },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
