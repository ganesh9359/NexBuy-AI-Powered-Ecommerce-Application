import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { AuthCallbackComponent } from './callback/callback.component';
import { ForgotComponent } from './forgot/forgot.component';
import { ResetComponent } from './reset/reset.component';
import { AuthHomeComponent } from './auth-home.component';

const routes: Routes = [
  { path: 'login', component: AuthHomeComponent, data: { mode: 'login' } },
  { path: 'register', component: AuthHomeComponent, data: { mode: 'register' } },
  { path: 'forgot', component: AuthHomeComponent, data: { mode: 'forgot' } },
  { path: 'reset', component: AuthHomeComponent, data: { mode: 'reset' } },
  { path: 'callback', component: AuthCallbackComponent },
  { path: '', component: AuthHomeComponent, data: { mode: 'login' } }
];

@NgModule({
  declarations: [LoginComponent, RegisterComponent, AuthCallbackComponent, ForgotComponent, ResetComponent, AuthHomeComponent],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule.forChild(routes)]
})
export class AuthModule {}