import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService, AuthResponse } from './auth/core/auth.service';
import { STORE_CATEGORIES } from './shared/store-categories';
import { environment } from '../environments/environment';
import { CatalogCategory, CatalogProductCard, ProductApiService } from './product/product-api.service';
import { AiChatResponse, AiOrderPreview, AiService } from './ai/ai.service';

type AuthMode = 'login' | 'register' | 'forgot' | 'reset';
type PasswordField = 'login' | 'register' | 'registerConfirm' | 'reset' | 'resetConfirm';
type AssistantLanguage = 'en' | 'hi' | 'mr';

interface ShellChatMessage {
  role: 'assistant' | 'user';
  text: string;
  headline?: string;
  products?: CatalogProductCard[];
  orders?: AiOrderPreview[];
  quickReplies?: string[];
  nextStep?: string;
}

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit, OnDestroy {
  title = 'frontend';
  navLinks = [
    { path: '/product', label: 'Home' },
    { path: '/product/catalog', label: 'Catalog' },
    { path: '/ai', label: 'AI' }
  ];
  storeCategories: CatalogCategory[] = this.buildFallbackCategories();
  searchQuery = '';
  isMenuOpen = false;
  isLight = false;
  voiceListening = false;
  imagePreview = '';
  selectedSearchImageFile?: File;
  searchBusy = false;
  searchStatus = '';
  chatbotOpen = false;
  chatLanguage: AssistantLanguage = 'en';
  chatInput = '';
  chatBusy = false;
  chatMessages: ShellChatMessage[] = [];

  showLoginModal = false;
  showProfileMenu = false;
  authMode: AuthMode = 'login';
  otpStep = false;
  pendingUserId: number | null = null;
  loginEmail = '';
  loginPassword = '';
  loginFirstName = '';
  loginLastName = '';
  loginPhone = '';
  addressLine1 = '';
  addressCity = '';
  addressState = '';
  addressPostal = '';
  addressCountry = '';
  loginOtp = '';
  registerConfirmPassword = '';
  resetPasswordValue = '';
  resetConfirmPassword = '';
  loginError = '';
  loginNotice = '';
  loggingIn = false;
  passwordVisibility: Record<PasswordField, boolean> = {
    login: false,
    register: false,
    registerConfirm: false,
    reset: false,
    resetConfirm: false
  };
  private recognition?: any;
  private readonly openChatbotHandler = () => this.openChatbot();
  private readonly currency = new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 2
  });

  constructor(
    private auth: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private productApi: ProductApiService,
    private aiService: AiService
  ) {}

  ngOnInit(): void {
    this.refreshAuthState();
    this.loadStoreCategories();
    this.chatMessages = [this.createAssistantWelcome()];
    window.addEventListener('nexbuy:open-chatbot', this.openChatbotHandler);

    this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe((event) => {
      const navigation = event as NavigationEnd;
      this.auth.rememberLastVisitedUrl(navigation.urlAfterRedirects);
      this.showProfileMenu = false;
      this.stopHeaderVoiceSearch();
      this.clearSelectedSearchImage(false);
      this.searchStatus = '';
    });

    this.route.queryParamMap.subscribe((params) => {
      const auth = params.get('auth');
      if (auth && this.isAuthMode(auth)) {
        this.openAuth(auth);
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: { auth: null },
          queryParamsHandling: 'merge',
          replaceUrl: true
        });
      }
    });
  }

  ngOnDestroy(): void {
    window.removeEventListener('nexbuy:open-chatbot', this.openChatbotHandler);
    this.stopHeaderVoiceSearch();
  }

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  get isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  get userEmail(): string | null {
    return localStorage.getItem('userEmail');
  }

  get isAdminLayout(): boolean {
    return this.router.url.startsWith('/admin');
  }

  get voiceSupported(): boolean {
    const speechCtor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    return !!speechCtor;
  }

  toggleTheme(): void {
    this.isLight = !this.isLight;
    const root = document.documentElement;
    if (this.isLight) {
      root.classList.add('light-theme');
    } else {
      root.classList.remove('light-theme');
    }
  }

  submitSearch(): void {
    const query = this.searchQuery.trim();
    this.isMenuOpen = false;
    this.searchStatus = '';
    if (this.searchBusy) {
      return;
    }
    if (this.selectedSearchImageFile) {
      this.searchByImage(query);
      return;
    }
    if (!query) {
      this.router.navigate(['/product/catalog']);
      return;
    }
    this.router.navigate(['/product/search'], { queryParams: { q: query } });
  }

  toggleVoiceSearch(): void {
    this.isMenuOpen = false;
    if (this.voiceListening) {
      this.stopHeaderVoiceSearch();
      this.searchStatus = this.searchQuery.trim() ? 'Voice captured. Press search to continue.' : '';
      return;
    }
    if (!this.voiceSupported) {
      this.searchStatus = 'Voice capture is not supported in this browser.';
      return;
    }

    const SpeechRecognitionCtor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    this.recognition = new SpeechRecognitionCtor();
    this.recognition.lang = this.speechLocale();
    this.recognition.interimResults = true;
    this.recognition.maxAlternatives = 1;
    this.voiceListening = true;
    this.searchStatus = 'Listening... tap the mic again to stop.';

    this.recognition.onresult = (event: any) => {
      const transcript = Array.from(event?.results || [])
        .map((result: any) => result?.[0]?.transcript || '')
        .join(' ')
        .trim();
      this.searchQuery = transcript;
    };

    this.recognition.onerror = () => {
      this.voiceListening = false;
      this.searchStatus = 'Voice capture failed. Try the mic again or type your search.';
    };

    this.recognition.onend = () => {
      this.voiceListening = false;
      if (this.searchQuery.trim()) {
        this.searchStatus = 'Voice captured. Press search to continue.';
      }
    };

    this.recognition.start();
  }

  stopHeaderVoiceSearch(): void {
    if (this.recognition) {
      try {
        this.recognition.stop();
      } catch {
        // no-op
      }
    }
    this.voiceListening = false;
  }

  openImagePicker(input: HTMLInputElement): void {
    this.isMenuOpen = false;
    input.click();
  }

  onSearchImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.selectedSearchImageFile = file;
    this.searchStatus = 'Image selected. Press search to match it.';

    const reader = new FileReader();
    reader.onload = () => {
      this.imagePreview = typeof reader.result === 'string' ? reader.result : '';
    };
    reader.readAsDataURL(file);
  }

  clearSelectedSearchImage(clearStatus = true): void {
    this.selectedSearchImageFile = undefined;
    this.imagePreview = '';
    if (clearStatus) {
      this.searchStatus = '';
    }
  }

  private searchByImage(hint: string): void {
    if (!this.selectedSearchImageFile || this.searchBusy) {
      return;
    }

    this.searchBusy = true;
    this.searchStatus = 'Matching the selected image...';

    this.aiService.imageSearch(this.selectedSearchImageFile, hint).subscribe({
      next: (response) => {
        const nextQuery = (response.extractedHint || hint || response.products?.[0]?.title || '').trim();
        this.searchBusy = false;
        this.clearSelectedSearchImage(false);
        this.searchQuery = nextQuery;
        this.searchStatus = '';

        if (nextQuery) {
          this.router.navigate(['/product/search'], { queryParams: { q: nextQuery } });
          return;
        }

        if (response.products?.length) {
          this.router.navigate(['/product', response.products[0].slug]);
          return;
        }

        this.router.navigate(['/product/catalog']);
      },
      error: (err) => {
        this.searchBusy = false;
        this.searchStatus = err?.error?.message || 'Image search could not run right now.';
      }
    });
  }

  openChatbot(): void {
    this.chatbotOpen = true;
  }

  toggleChatbot(): void {
    this.chatbotOpen = !this.chatbotOpen;
  }

  closeChatbot(): void {
    this.chatbotOpen = false;
  }

  setChatLanguage(language: AssistantLanguage): void {
    if (this.chatLanguage === language) {
      return;
    }
    this.chatLanguage = language;
    if (this.chatMessages.length === 1 && this.chatMessages[0]?.role === 'assistant') {
      this.chatMessages = [this.createAssistantWelcome()];
      return;
    }

    this.chatMessages = [
      ...this.chatMessages,
      {
        role: 'assistant',
        headline: this.languageTitle(language),
        text: this.languageNotice(language),
        quickReplies: this.languagePrompts(language)
      }
    ];
  }

  sendChat(prompt?: string): void {
    const message = (prompt ?? this.chatInput).trim();
    if (!message || this.chatBusy) {
      return;
    }

    this.chatbotOpen = true;
    this.chatMessages = [...this.chatMessages, { role: 'user', text: message }];
    this.chatInput = '';
    this.chatBusy = true;

    this.aiService.chat(message, this.chatLanguage).subscribe({
      next: (response) => {
        this.chatMessages = [...this.chatMessages, this.toAssistantEntry(response)];
        this.chatBusy = false;
      },
      error: (err) => {
        this.chatMessages = [
          ...this.chatMessages,
          {
            role: 'assistant',
            headline: this.chatLanguage === 'hi' ? 'Hindi assistant unavailable' : this.chatLanguage === 'mr' ? 'Marathi assistant unavailable' : 'Assistant unavailable',
            text: err?.error?.message || 'The assistant could not answer right now.',
            quickReplies: this.languagePrompts(this.chatLanguage)
          }
        ];
        this.chatBusy = false;
      }
    });
  }

  onProfileClick(): void {
    if (this.isLoggedIn) {
      this.showProfileMenu = !this.showProfileMenu;
    } else {
      this.auth.rememberReturnUrl(this.router.url);
      this.openAuth('login');
    }
  }

  openAuth(mode: AuthMode = 'login'): void {
    this.resetAuthState();
    this.authMode = mode;
    this.showLoginModal = true;
    this.showProfileMenu = false;
  }

  closeLogin(): void {
    this.showLoginModal = false;
  }

  logout(): void {
    this.auth.logout();
    this.showProfileMenu = false;
  }

  socialLogin(provider: string): void {
    window.location.href = `${environment.apiBase}/oauth2/authorize/${provider}`;
  }

  switchAuthMode(mode: AuthMode): void {
    const preservedEmail = this.loginEmail;
    this.resetAuthState();
    this.loginEmail = preservedEmail;
    this.authMode = mode;
    this.showLoginModal = true;
  }

  togglePassword(field: PasswordField): void {
    this.passwordVisibility[field] = !this.passwordVisibility[field];
  }

  submitAuth(): void {
    if (this.otpStep) {
      this.verifyOtp();
      return;
    }

    switch (this.authMode) {
      case 'login':
        this.submitLogin();
        break;
      case 'register':
        this.submitRegister();
        break;
      case 'forgot':
        this.submitForgot();
        break;
      case 'reset':
        this.submitReset();
        break;
    }
  }

  verifyOtp(): void {
    if (!this.pendingUserId || !this.loginOtp) {
      this.loginError = 'Enter the OTP sent to your email';
      return;
    }
    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.verifyOtp({ userId: this.pendingUserId, otp: this.loginOtp, purpose: 'register' }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => this.handleError(err)
    });
  }

  resendOtp(): void {
    if (!this.pendingUserId) return;
    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.resendOtp({ userId: this.pendingUserId, purpose: 'register' }).subscribe({
      next: () => {
        this.loggingIn = false;
        this.loginNotice = 'A fresh OTP is on the way.';
      },
      error: (err) => this.handleError(err)
    });
  }

  updatePendingEmail(): void {
    if (!this.pendingUserId || !this.loginEmail) {
      this.loginError = 'Enter the correct email address first';
      return;
    }

    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.updatePendingRegistrationEmail({ userId: this.pendingUserId, email: this.loginEmail }).subscribe({
      next: () => {
        this.loggingIn = false;
        this.loginOtp = '';
        this.loginNotice = 'Email updated and a fresh OTP is on the way.';
      },
      error: (err) => this.handleError(err)
    });
  }

  get modalTitle(): string {
    if (this.otpStep) return 'Verify your email';
    if (this.authMode === 'register') return 'Create account';
    if (this.authMode === 'forgot') return 'Forgot password';
    if (this.authMode === 'reset') return 'Reset password';
    return 'Welcome back';
  }

  get modalSubtitle(): string {
    if (this.otpStep) return 'Enter the OTP we sent to your email to activate your account. If the email is wrong, update it below and we will resend the code.';
    if (this.authMode === 'register') return 'Sign up to save your cart and get recommendations.';
    if (this.authMode === 'forgot') return 'Enter your email and we will send you a reset OTP.';
    if (this.authMode === 'reset') return 'Enter the OTP from your email and choose a new password that is different from the old one.';
    return 'Sign in to track orders, save your cart, and continue checkout.';
  }

  get submitLabel(): string {
    if (this.otpStep) return this.loggingIn ? 'Verifying...' : 'Verify OTP';
    if (this.authMode === 'register') return this.loggingIn ? 'Creating...' : 'Create account';
    if (this.authMode === 'forgot') return this.loggingIn ? 'Sending...' : 'Send OTP';
    if (this.authMode === 'reset') return this.loggingIn ? 'Resetting...' : 'Reset password';
    return this.loggingIn ? 'Signing in...' : 'Sign in';
  }

  private submitLogin(): void {
    if (!this.loginEmail || !this.loginPassword) {
      this.loginError = 'Please fill all required fields';
      return;
    }

    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.login({ email: this.loginEmail, password: this.loginPassword }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => this.handleError(err)
    });
  }

  private submitRegister(): void {
    if (!this.validateRegisterFields()) {
      this.loginError = 'Please fill all required fields';
      return;
    }
    if (this.loginPassword !== this.registerConfirmPassword) {
      this.loginError = 'Passwords do not match';
      return;
    }

    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.register({
      firstName: this.loginFirstName,
      lastName: this.loginLastName,
      phone: this.loginPhone,
      email: this.loginEmail,
      password: this.loginPassword,
      confirmPassword: this.registerConfirmPassword,
      line1: this.addressLine1,
      city: this.addressCity,
      state: this.addressState,
      postalCode: this.addressPostal,
      country: this.addressCountry
    }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => this.handleError(err)
    });
  }

  private submitForgot(): void {
    if (!this.loginEmail) {
      this.loginError = 'Enter your email first';
      return;
    }

    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.forgot({ email: this.loginEmail }).subscribe({
      next: () => {
        this.loggingIn = false;
        this.loginNotice = 'Reset OTP sent to your email.';
        this.authMode = 'reset';
        this.loginOtp = '';
        this.resetPasswordValue = '';
        this.resetConfirmPassword = '';
      },
      error: (err) => this.handleError(err)
    });
  }

  private submitReset(): void {
    if (!this.loginEmail || !this.loginOtp || !this.resetPasswordValue || !this.resetConfirmPassword) {
      this.loginError = 'Please fill all required fields';
      return;
    }
    if (this.resetPasswordValue !== this.resetConfirmPassword) {
      this.loginError = 'Passwords do not match';
      return;
    }

    this.loggingIn = true;
    this.loginError = '';
    this.loginNotice = '';
    this.auth.reset({
      email: this.loginEmail,
      otp: this.loginOtp,
      newPassword: this.resetPasswordValue,
      confirmPassword: this.resetConfirmPassword
    }).subscribe({
      next: (res) => this.handleAuthResponse(res),
      error: (err) => this.handleError(err)
    });
  }

  private validateRegisterFields(): boolean {
    return !!(
      this.loginEmail &&
      this.loginPassword &&
      this.registerConfirmPassword &&
      this.loginFirstName &&
      this.addressLine1 &&
      this.addressCity &&
      this.addressState &&
      this.addressPostal &&
      this.addressCountry
    );
  }

  private handleAuthResponse(res: AuthResponse): void {
    this.loggingIn = false;
    if (res.requiresOtp) {
      this.otpStep = true;
      this.pendingUserId = res.userId || null;
      this.loginOtp = '';
      this.loginNotice = 'We sent an OTP to your email.';
      return;
    }

    this.otpStep = false;
    this.pendingUserId = null;
    this.showLoginModal = false;
    this.refreshAuthState();
    const role = (res.role || this.auth.userRole || '').toUpperCase();
    const returnUrl = this.auth.consumeReturnUrl();
    const target = role === 'ADMIN' ? '/admin' : (returnUrl || '/product');
    this.router.navigateByUrl(target);
  }

  private handleError(err: any): void {
    this.loggingIn = false;
    this.loginNotice = '';
    this.loginError = err?.error?.message || 'Something went wrong';
  }

  private resetAuthState(): void {
    this.loginError = '';
    this.loginNotice = '';
    this.loginPassword = '';
    this.loginFirstName = '';
    this.loginLastName = '';
    this.loginPhone = '';
    this.addressLine1 = '';
    this.addressCity = '';
    this.addressState = '';
    this.addressPostal = '';
    this.addressCountry = '';
    this.loginOtp = '';
    this.registerConfirmPassword = '';
    this.resetPasswordValue = '';
    this.resetConfirmPassword = '';
    this.otpStep = false;
    this.pendingUserId = null;
    this.passwordVisibility = {
      login: false,
      register: false,
      registerConfirm: false,
      reset: false,
      resetConfirm: false
    };
  }

  private isAuthMode(value: string): value is AuthMode {
    return value === 'login' || value === 'register' || value === 'forgot' || value === 'reset';
  }

  private refreshAuthState(): void {
    // reserved for future reactive auth state
  }

  private loadStoreCategories(): void {
    this.productApi.getCategories().subscribe({
      next: (categories) => {
        if (categories.length) {
          this.storeCategories = categories;
        }
      },
      error: () => {
        this.storeCategories = this.buildFallbackCategories();
      }
    });
  }

  formatPrice(cents?: number | null): string {
    return this.currency.format((cents ?? 0) / 100);
  }

  formatShortDate(value?: string): string {
    if (!value) {
      return this.chatLanguage === 'hi' ? 'अभी' : this.chatLanguage === 'mr' ? 'आत्ताच' : 'Just now';
    }
    return new Intl.DateTimeFormat('en-IN', {
      day: 'numeric',
      month: 'short',
      hour: 'numeric',
      minute: '2-digit'
    }).format(new Date(value));
  }

  trackBySlug(_index: number, product: CatalogProductCard): string {
    return product.slug;
  }

  trackByText(_index: number, value: string): string {
    return value;
  }

  private buildFallbackCategories(): CatalogCategory[] {
    return STORE_CATEGORIES.map((category, index) => ({
      id: index + 1,
      name: category.name,
      slug: this.slugify(category.name),
      description: category.items.join(' ? '),
      imageUrl: category.image,
      productCount: 0
    }));
  }

  private slugify(value: string): string {
    return value
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-+|-+$/g, '');
  }

  private speechLocale(): string {
    return this.chatLanguage === 'hi' ? 'hi-IN' : this.chatLanguage === 'mr' ? 'mr-IN' : 'en-IN';
  }

  private createAssistantWelcome(): ShellChatMessage {
    return {
      role: 'assistant',
      headline: this.languageTitle(this.chatLanguage),
      text: this.languageWelcome(this.chatLanguage),
      quickReplies: this.languagePrompts(this.chatLanguage),
      nextStep: this.languageNextStep(this.chatLanguage)
    };
  }

  private toAssistantEntry(response: AiChatResponse): ShellChatMessage {
    return {
      role: 'assistant',
      headline: response.headline,
      text: response.answer,
      products: response.products,
      orders: response.orders,
      quickReplies: response.quickReplies,
      nextStep: response.nextStep
    };
  }

  private languageTitle(language: AssistantLanguage): string {
    return language === 'hi' ? 'NexBuy assistant - Hindi' : language === 'mr' ? 'NexBuy assistant - Marathi' : 'NexBuy assistant';
  }

  private languageWelcome(language: AssistantLanguage): string {
    if (language === 'hi') {
      return 'I can help you shop in Hindi, including products, deals, order tracking, and buying guidance.';
    }
    if (language === 'mr') {
      return 'I can help you shop in Marathi, including products, deals, order tracking, and buying guidance.';
    }
    return 'I can help you find products, compare deals, track orders, and make better buying decisions.';
  }

  private languageNextStep(language: AssistantLanguage): string {
    if (language === 'hi') {
      return 'You can type in Hindi, Marathi, or English.';
    }
    if (language === 'mr') {
      return 'You can type in Marathi, Hindi, or English.';
    }
    return 'You can ask in English, Hindi, or Marathi.';
  }

  private languagePrompts(language: AssistantLanguage): string[] {
    if (language === 'hi') {
      return ['Show phones under 30000', 'Track my latest order', 'Show best deals today'];
    }
    if (language === 'mr') {
      return ['Show phones under 30000', 'Track my latest order', 'Show best deals today'];
    }
    return ['Recommend phones under 30000', 'Track my latest order', "Show today's best deals"];
  }

  private languageNotice(language: AssistantLanguage): string {
    if (language === 'hi') {
      return 'Okay, I will continue helping in Hindi.';
    }
    if (language === 'mr') {
      return 'Okay, I will continue helping in Marathi.';
    }
    return 'Sure, I will help in English now.';
  }
}
