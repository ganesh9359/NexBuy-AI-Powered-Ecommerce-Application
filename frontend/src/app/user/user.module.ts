import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { UserHomeComponent } from './user-home.component';

const routes: Routes = [
  { path: '', component: UserHomeComponent }
];

@NgModule({
  declarations: [UserHomeComponent],
  imports: [CommonModule, RouterModule.forChild(routes)]
})
export class UserModule {}
