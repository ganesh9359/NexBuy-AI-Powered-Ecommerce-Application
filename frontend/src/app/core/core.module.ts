import { NgModule, Optional, SkipSelf } from '@angular/core';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AuthInterceptor } from '../auth/core/auth.interceptor';

@NgModule({
  imports: [HttpClientModule],
  providers: [{ provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true }]
})
export class CoreModule {
  constructor(@Optional() @SkipSelf() parent?: CoreModule) {
    if (parent) {
      throw new Error('CoreModule should only be imported once (in AppModule).');
    }
  }
}