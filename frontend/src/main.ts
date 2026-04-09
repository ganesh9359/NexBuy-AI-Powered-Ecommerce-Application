import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import { AppModule } from './app/app.module';

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => {
    // Use a safe error handling mechanism instead of console.error to prevent log injection
    const errorMessage = err instanceof Error ? err.message : String(err);
    console.error('Bootstrap error:', errorMessage);
  });
