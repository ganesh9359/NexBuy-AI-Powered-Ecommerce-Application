import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProductHomeComponent } from './product-home.component';
import { ProductCatalogComponent } from './product-catalog.component';
import { ProductDetailComponent } from './product-detail.component';

const routes: Routes = [
  { path: '', component: ProductHomeComponent },
  { path: 'catalog', component: ProductCatalogComponent, data: { mode: 'catalog' } },
  { path: 'category/:slug', component: ProductCatalogComponent, data: { mode: 'category' } },
  { path: 'search', component: ProductCatalogComponent, data: { mode: 'search' } },
  { path: ':slug', component: ProductDetailComponent }
];

@NgModule({
  declarations: [ProductHomeComponent, ProductCatalogComponent, ProductDetailComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class ProductModule {}
