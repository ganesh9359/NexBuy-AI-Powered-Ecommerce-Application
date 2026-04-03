import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-auth-callback',
  template: '<p class="muted">Signing you in...</p>'
})
export class AuthCallbackComponent implements OnInit {
  constructor(private route: ActivatedRoute, private auth: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((p) => {
      const token = p['token'];
      const refreshToken = p['refreshToken'];
      const email = p['email'] || '';
      if (token) {
        this.auth.setAuth({ token, refreshToken, email });
      }
      const target = this.auth.isAdmin() ? '/admin' : (this.auth.consumeReturnUrl() || '/product');
      this.router.navigateByUrl(target);
    });
  }
}
