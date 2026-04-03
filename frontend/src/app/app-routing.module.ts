import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './auth/core/auth.guard';
import { AdminGuard } from './auth/core/admin.guard';

const routes: Routes = [
  { path: '', redirectTo: 'product', pathMatch: 'full' },
  { path: 'product', loadChildren: () => import('./product/product.module').then((m) => m.ProductModule) },
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then((m) => m.AuthModule) },
  { path: 'user', loadChildren: () => import('./user/user.module').then((m) => m.UserModule), canActivate: [AuthGuard] },
  { path: 'cart', loadChildren: () => import('./cart/cart.module').then((m) => m.CartModule), canActivate: [AuthGuard] },
  { path: 'order', loadChildren: () => import('./order/order.module').then((m) => m.OrderModule), canActivate: [AuthGuard] },
  { path: 'payment', loadChildren: () => import('./payment/payment.module').then((m) => m.PaymentModule), canActivate: [AuthGuard] },
  { path: 'admin', loadChildren: () => import('./admin/admin.module').then((m) => m.AdminModule), canActivate: [AuthGuard, AdminGuard] },
  { path: 'ai', loadChildren: () => import('./ai/ai.module').then((m) => m.AiModule) },
  { path: 'integration', loadChildren: () => import('./integration/integration.module').then((m) => m.IntegrationModule) },
  { path: '**', redirectTo: 'product' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes, { scrollPositionRestoration: 'enabled' })],
  exports: [RouterModule]
})
export class AppRoutingModule {}
