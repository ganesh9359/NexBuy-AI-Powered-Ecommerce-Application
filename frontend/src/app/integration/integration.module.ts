import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { IntegrationHomeComponent } from './integration-home.component';

const routes: Routes = [
  { path: '', component: IntegrationHomeComponent }
];

@NgModule({
  declarations: [IntegrationHomeComponent],
  imports: [CommonModule, RouterModule.forChild(routes)]
})
export class IntegrationModule {}
