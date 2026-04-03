import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { AiHomeComponent } from './ai-home.component';

const routes: Routes = [
  { path: '', component: AiHomeComponent }
];

@NgModule({
  declarations: [AiHomeComponent],
  imports: [CommonModule, FormsModule, RouterModule.forChild(routes)]
})
export class AiModule {}