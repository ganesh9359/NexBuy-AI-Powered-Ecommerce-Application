import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-auth-home',
  templateUrl: './auth-home.component.html',
  styleUrls: ['./auth-home.component.scss']
})
export class AuthHomeComponent implements OnInit {
  constructor(private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    const mode = this.route.snapshot.data['mode'] || 'login';
    this.router.navigate(['/'], {
      queryParams: { auth: mode },
      replaceUrl: true
    });
  }
}
