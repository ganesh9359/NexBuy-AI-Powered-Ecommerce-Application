import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { CartHomeComponent } from './cart-home.component';

const routes: Routes = [
  { path: '', component: CartHomeComponent }
];

@NgModule({
  declarations: [CartHomeComponent],
  imports: [CommonModule, RouterModule.forChild(routes)]
})
export class CartModule {}
